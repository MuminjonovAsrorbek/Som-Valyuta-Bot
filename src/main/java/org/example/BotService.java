package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.Actions.Actions;
import org.example.BotInfo.InfoBot;
import org.example.File.FileUtil;
import org.example.Logger.BotLogger;
import org.example.userData.BallTime;
import org.example.userData.User;
import org.example.userData.UserStep;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;


public class BotService extends TelegramLongPollingBot {

    private static final String BOT_TOKEN = InfoBot.BOT_TOKEN;
    private static final String BOT_USERNAME = InfoBot.BOT_USERNAME;

    private static final String FILE_PATH = "users.json";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final List<User> users = new ArrayList<>();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static Actions actions = new Actions();

    public BotService() {
        super(BOT_TOKEN);
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {

            processMessage(update.getMessage());

        } else if (update.hasCallbackQuery()) {

            processCallbackQuery(update.getCallbackQuery());

        }

    }

    private void processCallbackQuery(CallbackQuery callbackQuery) {

        String callback = callbackQuery.getData();

        Long chatId = callbackQuery.getMessage().getChatId();

        User user = getUser(chatId);

        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (Objects.requireNonNull(user).getUserStep().equals(UserStep.SELECT_MENU)) {

            switch (callback) {
                case "update" -> {

                    LocalTime time = LocalTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    formatter.format(time);

                    AnswerCallbackQuery answer = new AnswerCallbackQuery();
                    answer.setCallbackQueryId(callbackQuery.getId());
                    answer.setText("Yangilangan vaqti : " + time.format(formatter));

                    sendMessageOfCurrencies(chatId, Objects.requireNonNull(user));

                    try {
                        execute(answer);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }

                    deleteMessage(chatId, messageId);
                }
                case "ball-time-change" -> {

                    deleteMessage(chatId, messageId);

                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("\uD83D\uDD54 Iltimos, sizga qulay bo‘lgan vaqtdan birini tanlang : ");
                    message.setParseMode("HTML");

                    message.setReplyMarkup(replyKeyboardForTimeSelection());

                    myExecute(message);

                    user.getBallTime().setChanged(false);

                    user.setUserStep(UserStep.SELECT_TIME);
                }
                case "ball-time-off" -> {

                    deleteMessage(chatId, messageId);

                    cancelUserSchedule(user, chatId);

                }
                case "convert-first-menu" -> {
                    List<String> userAllCurrency = user.getAllCurrency();

                    String firstCur = user.getSelectedCurrency()[0];

                    String secondCur = user.getSelectedCurrency()[1];

                    updateInlineKeyboardForConvertFirst(firstCur, secondCur, chatId, messageId, userAllCurrency);

//                user.setUserStep(UserStep.SELECT_CURRENCY);

                }
                case "convert-second-menu" -> {

                    List<String> userAllCurrency = user.getAllCurrency();

                    String firstCur = user.getSelectedCurrency()[0];
                    String secondCur = user.getSelectedCurrency()[1];

                    updateInlineKeyboardForConvertSecond(firstCur, secondCur, chatId, messageId, userAllCurrency);

                }
                case "convert-swap" -> {

                    String[] selectedCurrency = user.getSelectedCurrency();

                    String[] newCurrency = new String[selectedCurrency.length];

                    newCurrency[0] = selectedCurrency[1];
                    newCurrency[1] = selectedCurrency[0];

                    user.setSelectedCurrency(newCurrency);

                    sendEditMessageOfCalculate(chatId, messageId, user);
                }
                case "convert-save" -> {
                    try {
                        EditMessageText editMessageText = new EditMessageText();
                        editMessageText.setChatId(chatId);
                        editMessageText.setMessageId(messageId);

                        String[] selectedCurrency = user.getSelectedCurrency();

                        editMessageText.setText("✅ <b> Valyuta saqlandi!</b> Endi hisob-kitob " +
                                getFlag(selectedCurrency[0]) + " " + selectedCurrency[0] + " \uD83D\uDD1C " +
                                getFlag(selectedCurrency[1]) + " " + selectedCurrency[1] + " asosida bo'ladi .\n");

                        editMessageText.setParseMode("HTML");

                        execute(editMessageText);

                        SendMessage message = new SendMessage();

                        message.setChatId(chatId);
                        message.setText("Iltimos qiymatni kiriting :");

                        cancelButton(message);

                        myExecute(message);

                        user.setUserStep(UserStep.ENTER_NUMBERS);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
                default -> {

                    String[] split = callback.split("-");

                    if (split[0].equals("first")) {

                        String cur = split[1];

                        String[] selectedCurrency = user.getSelectedCurrency();

                        selectedCurrency[0] = cur;

                        user.setSelectedCurrency(selectedCurrency);

                        sendEditMessageOfCalculate(chatId, messageId, user);

                    } else if (split[0].equals("second")) {

                        String cur = split[1];

                        String[] selectedCurrency = user.getSelectedCurrency();

                        selectedCurrency[1] = cur;

                        user.setSelectedCurrency(selectedCurrency);

                        sendEditMessageOfCalculate(chatId, messageId, user);

                    }
                }
            }

        } else if (user.getUserStep().equals(UserStep.IN_FAVORITE_CURRENCIES_MENU)) {

            if (callback.startsWith("toggle_")) {

                String currency = callback.split("_")[1];

                toggleCurrencySelection(chatId, messageId, currency, user, callbackQuery);

            } else if ("nextPage".equals(callback)) {

                changePage(chatId, messageId, 1, user);

            } else if ("prevPage".equals(callback)) {

                changePage(chatId, messageId, -1, user);

            } else if ("saveSelection".equals(callback)) {

                saveUserSelection(chatId, messageId);

            } else if ("clearSelection".equals(callback)) {

                clearUserSelection(chatId, messageId, user);

            }

        }


    }

    private void cancelUserSchedule(User user, Long chatId) {
        if (user.getUserTask() != null) {
            user.getUserTask().cancel(true);
            user.setUserTask(null);
        }

        user.setBallTime(new BallTime("08:00", false));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("❌ Obuna bekor qilindi.");
        menuKeyboard(sendMessage, user);
        myExecute(sendMessage);
    }

    private void clearUserSelection(Long chatId, Integer messageId, User user) {

        if (user.getAllCurrency().size() != 1) {

            List<String> newCurrency = new ArrayList<>();
            newCurrency.add("UZS");

            user.setAllCurrency(newCurrency);

            user.setCurrencySize(8);

            user.setCurrentPage(0);

            editCurrencySelection(chatId, messageId, 0, user);
        }

    }

    private void saveUserSelection(Long chatId, Integer messageId) {

        deleteMessage(chatId, messageId);

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId);

        sendMessage.setText("✅ <b>Sevimli valyutalaringiz saqlandi!</b>");

        sendMessage.setParseMode("HTML");

        myExecute(sendMessage);

    }

    public void changePage(Long chatId, Integer messageId, int direction, User user) {
        int currentPage = user.getCurrentPage();
        user.setCurrentPage(currentPage + direction);
        editCurrencySelection(chatId, messageId, currentPage + direction, user);
    }

    public void toggleCurrencySelection(Long chatId, Integer messageId, String currency, User user, CallbackQuery callbackQuery) {


        List<String> selected = user.getAllCurrency();

        if (selected.contains(currency)) {
            selected.remove(currency); // O‘chirish
            user.setCurrencySize(user.getCurrencySize() + 1);
        } else {

            if (user.getCurrencySize() > 0) {

                selected.add(currency); // Qo‘shish

                user.setCurrencySize(user.getCurrencySize() - 1);
            } else {

                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(callbackQuery.getId());
                answer.setText("Siz Maximum " + (user.getAllCurrency().size() - 1) + " ta valyuta tanladingiz !");

                try {
                    execute(answer);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        user.setAllCurrency(selected);

        editCurrencySelection(chatId, messageId, user.getCurrentPage(), user); // Sahifani yangilash

    }

    public void editCurrencySelection(Long chatId, Integer messageId, int currentPage, User user) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText("\uD83D\uDCCC Sevimli valyutalaringizni tanlang: \n\n" +
                "Maximum <b>" + (user.getCurrencySize()) + "</b> ta valyuta tanlay olasiz !");
        editMessageText.setParseMode("HTML");
        editMessageText.setReplyMarkup(getCurrencySelectionKeyboard(currentPage, user.getAllCurrency()));

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void cancelButton(SendMessage sendMessage) {

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("\uD83D\uDD19 Orqaga");

        keyboardRows.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

    }

    private void sendEditMessageOfCalculate(Long chatId, Integer messageId, User user) {

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);

        editMessage.setText("""
                \uD83D\uDCB1 <b>Valyuta konvertori</b> \s
                
                \uD83D\uDCCC Bu yerda valyutalarni oson almashtirishingiz mumkin: \s
                \uD83D\uDD39 <b>USD → UZS</b> (Dollar → So‘m) \s
                \uD83D\uDD39 <b>UZS → USD</b> (So‘m → Dollar) \s
                
                \uD83D\uDD3D <b>Konvertatsiya qilmoqchi bo‘lgan valyutalaringizni tanlang</b> va <b>Saqlash ✅</b> tugmasini bosing!
                """);

        editMessage.setParseMode("HTML");

        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardForCalculateMenu(user);

        editMessage.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    private void updateInlineKeyboardForConvertSecond(String firstCur, String secondCur, Long chatId, Integer messageId, List<String> userAllCurrency) {

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        int i = 0;

        while (i < userAllCurrency.size()) {

            List<InlineKeyboardButton> row = new ArrayList<>();

            int j = 0;
            while (j < 3 && i < userAllCurrency.size()) {

                InlineKeyboardButton button = new InlineKeyboardButton();

                if (userAllCurrency.get(i).equals(secondCur)) {

                    button.setText(getFlag(secondCur) + userAllCurrency.get(i) + "✅");

                } else if (userAllCurrency.get(i).equals(firstCur)) {

                    i++;
                    continue;

                } else {

                    button.setText(getFlag(userAllCurrency.get(i)) + userAllCurrency.get(i));

                }
                button.setCallbackData("second-" + userAllCurrency.get(i));

                row.add(button);

                i++;
                j++;
            }

            rowList.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(rowList);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText("2️⃣ Marhamat birinchi valyutani tanlang \uD83D\uDC47");

        editMessageText.setParseMode("HTML");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateInlineKeyboardForConvertFirst(String firstCur, String secondCur, Long chatId, Integer messageId, List<String> userAllCurrency) {

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        int i = 0;

        while (i < userAllCurrency.size()) {

            List<InlineKeyboardButton> row = new ArrayList<>();

            int j = 0;
            while (j < 3 && i < userAllCurrency.size()) {

                InlineKeyboardButton button = new InlineKeyboardButton();

                if (userAllCurrency.get(i).equals(firstCur)) {

                    button.setText(getFlag(firstCur) + userAllCurrency.get(i) + "✅");

                } else if (userAllCurrency.get(i).equals(secondCur)) {

                    i++;

                    continue;

                } else {

                    button.setText(getFlag(userAllCurrency.get(i)) + userAllCurrency.get(i));

                }
                button.setCallbackData("first-" + userAllCurrency.get(i));

                row.add(button);

                i++;
                j++;
            }

            rowList.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(rowList);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText("1️⃣ Marhamat birinchi valyutani tanlang \uD83D\uDC47");

        editMessageText.setParseMode("HTML");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            BotLogger.logError("Error while deleting message", e);
            throw new RuntimeException(e);
        }
    }


    private void processMessage(Message message) {

        User user = checkedUser(message);

        if (message.hasText()) {

            String text = message.getText().trim();

            if (text.equals("/start")) {

                sendMessageOfStart(message.getChatId(), user);

            } else if (user.getUserStep().equals(UserStep.SELECT_MENU)) {

                switch (text) {
                    case "\uD83D\uDCB0 Kurs" -> sendMessageOfCurrencies(message.getChatId(), user);
                    case "\uD83D\uDD14 Qo‘ng‘iroqcha" -> {

                        if (!user.getBallTime().getChanged()) {

                            sendTimeSelectionKeyboard(message.getChatId());

                            user.setUserStep(UserStep.SELECT_TIME);

                        } else {

                            sendAlreadyTimeMessage(message.getChatId(), user);

                        }
                    }
                    case "\uD83D\uDD22 Hisoblash" -> sendMessageOfCalculate(message, user);
                    case "⚙️ Sozlamalar" -> sendMessageOfSettings(message.getChatId(), user);
                    case "\uD83D\uDC51 Premium" -> sendMessageOfPremium(message.getChatId(), user);
                    case "\uD83D\uDCD6 Yo'riqnoma" -> sendMessageOfGuide(message.getChatId());
                    case "\uD83D\uDCDE Biz bilan aloqa" -> sendMessageOfContactToAdmin(message.getChatId(), user);
                    default -> {

                        SendMessage sendMessage = new SendMessage();

                        wrongAnswer(message, sendMessage);

                        menuKeyboard(sendMessage, user);

                        myExecute(sendMessage);
                    }
                }

            } else if (user.getUserStep().equals(UserStep.SELECT_TIME)) {

                if (!user.getBallTime().getChanged()) {
                    Pattern pattern = Pattern.compile("(\\d+):(\\d+)");

                    if (pattern.matcher(text).matches()) {

                        user.setBallTime(new BallTime(text, true));

                        sendMessageScheduleTime(user, message);
                    } else {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(message.getChatId());
                        sendMessage.setText("""
                                ⚠️ <b> Noto'g'ri format !</b>
                                
                                Iltimos pastdagi tugmalardan foydalaning \uD83D\uDC47\s
                                """);
                        sendMessage.setParseMode("HTML");

                        sendMessage.setReplyMarkup(replyKeyboardForTimeSelection());
                        myExecute(sendMessage);
                    }
                }

            } else if (user.getUserStep().equals(UserStep.ENTER_NUMBERS)) {

                String regex = "^[0-9]+([.,][0-9]+)?$";

                if (text.equals("\uD83D\uDD19 Orqaga")) {

                    backMessage(message, user);

                } else if (text.matches(regex)) {

                    String[] selectedCurrency = user.getSelectedCurrency();

                    String firstCur = selectedCurrency[0];
                    String secondCur = selectedCurrency[1];

                    text = text.replace(',', '.');

                    if (firstCur.equals("UZS") || secondCur.equals("UZS")) {

                        if (firstCur.equals("UZS")) {

                            double currencyRate = actions.getCurrencyRate(secondCur);

                            double sumCurrency = Double.parseDouble(text);

                            double result = sumCurrency / currencyRate;

                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(message.getChatId().toString());
                            sendMessage.setText("\uD83D\uDCB1 <b>Konvertatsiya natijasi</b> \uD83D\uDCB1 \n\n" +
                                    "✅ Siz kiritgan summa: \n<b>" + rateDoubleToString(sumCurrency) + "</b> " + firstCur + getFlag(firstCur) + "\n\n" +
                                    "\uD83D\uDD04 Hozirgi kurs bo‘yicha: \n<b>" + (secondCur.equals("IRR") ? 10 : 1) + " " +
                                    secondCur + getFlag(secondCur) + "</b> = <b>" + rateDoubleToString(currencyRate) + " " + firstCur +
                                    getFlag(firstCur) + "</b> \n\n" +
                                    "\uD83D\uDCCA Hisoblangan natija: <b>" + rateDoubleToString(result) + "</b> " + actions.getCurrencyNameUz(secondCur));

                            sendMessage.setParseMode("HTML");
                            myExecute(sendMessage);

                        } else {

                            double currencyRate = actions.getCurrencyRate(firstCur);

                            double usdCurrency = Double.parseDouble(text);

                            double result = currencyRate * usdCurrency;

                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(message.getChatId().toString());
                            sendMessage.setText("\uD83D\uDCB1 <b>Konvertatsiya natijasi</b> \uD83D\uDCB1 \n\n" +
                                    "✅ Siz kiritgan summa: \n<b>" + rateDoubleToString(usdCurrency) + "</b> " + firstCur + getFlag(firstCur) + "\n\n" +
                                    "\uD83D\uDD04 Hozirgi kurs bo‘yicha: \n<b>" + (firstCur.equals("IRR") ? 10 : 1) + " " +
                                    firstCur + getFlag(firstCur) + "</b> = <b>" + rateDoubleToString(currencyRate) + " " + secondCur +
                                    getFlag(secondCur) + "</b> \n\n" +
                                    "\uD83D\uDCCA Hisoblangan natija: <b>" + rateDoubleToString(result) + "</b> " + "O'zbek so'mi");

                            sendMessage.setParseMode("HTML");
                            myExecute(sendMessage);

                        }

                    } else {

                        double base = actions.getCurrencyRate(firstCur);
                        double target = actions.getCurrencyRate(secondCur);

                        double userCur = Double.parseDouble(text);

                        double result = (userCur * base) / target;

                        SendMessage sendMessage = new SendMessage();

                        sendMessage.setChatId(message.getChatId().toString());

                        sendMessage.setText("\uD83D\uDCB1 <b>Konvertatsiya natijasi</b> \uD83D\uDCB1 \n\n" +
                                "\uD83D\uDD04 <b>" + rateDoubleToString(userCur) + " " + firstCur + " ≈ " +
                                rateDoubleToString(result) + " " + secondCur + "</b>");

                        sendMessage.setParseMode("HTML");
                        myExecute(sendMessage);

                    }


                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(message.getChatId().toString());
                    sendMessage.setText("❌ <b> Iltimos faqat raqam kiriting ! </b>");
                    sendMessage.setParseMode("HTML");
                    myExecute(sendMessage);
                }

            } else if (user.getUserStep().equals(UserStep.IN_SETTINGS_MENU)) {

                if (text.equals("\uD83D\uDCCC Sevimli valyutalar")) {

                    sendMessageForFavoriteCurrencies(message.getChatId(), user);

                } else if (text.equals("\uD83D\uDD19 Ortga qaytish")) {

                    backMessage(message, user);

                } else {

                    SendMessage sendMessage = new SendMessage();

                    wrongAnswer(message, sendMessage);

                    sendMessage.setReplyMarkup(replyKeyboardForSettingsMenu());

                    myExecute(sendMessage);

                }

            } else if (user.getUserStep().equals(UserStep.IN_FAVORITE_CURRENCIES_MENU)) {

                switch (text) {
                    case "\uD83D\uDC41 Ko'rish" ->
                            sendMessageForFavoriteCurrenciesInSeeCurrencies(message.getChatId(), user);
                    case "\uD83D\uDD1D Asosiy menyuga" -> backMessage(message, user);
                    case "\uD83D\uDD19 Ortga qaytish" -> {

                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(message.getChatId().toString());
                        sendMessage.setText("Menu");
                        sendMessage.setReplyMarkup(replyKeyboardForSettingsMenu());
                        user.setUserStep(UserStep.IN_SETTINGS_MENU);

                        myExecute(sendMessage);

                    }
                    case "➕ Qo'shish" -> senMessageOfAdd(message.getChatId(), user);
                    default -> {

                        SendMessage sendMessage = new SendMessage();

                        wrongAnswer(message, sendMessage);

                        sendMessage.setReplyMarkup(replyKeyboardForFavoriteCurrenciesMenu());

                        myExecute(sendMessage);

                    }
                }

            } else if (user.getUserStep().equals(UserStep.WAITING_FOR_MESSAGE)) {

                if (text.equals("\uD83D\uDD19 Orqaga")) {

                    backMessage(message, user);

                    user.setUserStep(UserStep.SELECT_MENU);

                } else {

                    try {

                        forwardUserMessageToAdmin(message);

                        sendMessage(message.getChatId().toString(), user);

                        user.setUserStep(UserStep.SELECT_MENU);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }

                }

            }

        } else if (user.getUserStep().equals(UserStep.WAITING_FOR_MESSAGE)) {

            try {

                forwardUserMessageToAdmin(message);

                sendMessage(message.getChatId().toString(), user);

                user.setUserStep(UserStep.SELECT_MENU);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

        }

    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void forwardUserMessageToAdmin(Message message) throws TelegramApiException {
        String adminChatId = InfoBot.ADMIN_CHAT_ID;
        Long userId = message.getFrom().getId();
        String firstName = escapeHtml(message.getFrom().getFirstName());
        String username = escapeHtml(message.getFrom().getUserName());
        String userMessage = message.hasText() ? escapeHtml(message.getText()) : null;
        String caption = message.getCaption() != null ? escapeHtml(message.getCaption()) : null;

        String header = "📩 <b>Yangi xabar!</b>\n\n" +
                "👤 <b>Foydalanuvchi:</b> @" + (username != null ? username : "Username yo‘q") + "\n" +
                "👤 <b>Ismi:</b> " + firstName + "\n" +
                "🆔 <b>ID:</b> <code>" + userId + "</code>\n";


        if (message.hasText()) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(adminChatId);
            sendMessage.setText(header + "\n💬 <b>Xabar:</b>\n" + userMessage);
            sendMessage.setParseMode("HTML");
            myExecute(sendMessage);
        } else if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            String fileId = photos.getLast().getFileId();

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(adminChatId);
            sendPhoto.setPhoto(new InputFile(fileId));

            if (caption != null) {
                sendPhoto.setCaption(header + "\n💬 <b>Xabar:</b>\n" + caption);
                sendPhoto.setParseMode("HTML");
            } else {
                sendPhoto.setCaption(header);
            }

            execute(sendPhoto);
        } else if (message.hasVoice()) {
            String fileId = message.getVoice().getFileId();

            SendVoice sendVoice = new SendVoice();
            sendVoice.setChatId(adminChatId);
            sendVoice.setVoice(new InputFile(fileId));

            if (caption != null) {
                sendVoice.setCaption(header + "\n💬 <b>Xabar:</b>\n" + caption);
                sendVoice.setParseMode("HTML");
            } else {
                sendVoice.setCaption(header);
            }

            execute(sendVoice);
        } else if (message.hasVideo()) {
            String fileId = message.getVideo().getFileId();

            SendVideo sendVideo = new SendVideo();
            sendVideo.setChatId(adminChatId);
            sendVideo.setVideo(new InputFile(fileId));

            if (caption != null) {
                sendVideo.setCaption(header + "\n💬 <b>Xabar:</b>\n" + caption);
                sendVideo.setParseMode("HTML");
            } else {
                sendVideo.setCaption(header);
            }

            execute(sendVideo);
        } else if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(adminChatId);
            sendDocument.setDocument(new InputFile(fileId));

            if (caption != null) {
                sendDocument.setCaption(header + "\n💬 <b>Xabar:</b>\n" + caption);
                sendDocument.setParseMode("HTML");
            } else {
                sendDocument.setCaption(header);
            }

            execute(sendDocument);
        }
    }


    private static void wrongAnswer(Message message, SendMessage sendMessage) {

        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("""
                ⚠️ <b> Noto'g'ri format !</b>
                
                Iltimos pastdagi tugmalardan foydalaning \uD83D\uDC47\s
                """);

        sendMessage.setParseMode("HTML");
    }

    private void sendMessage(String chatId, User user) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("✅ Xabaringiz yuborildi! Rahmat! 😊");
        sendMessage.setParseMode("HTML");

        menuKeyboard(sendMessage, user);

        myExecute(sendMessage);

    }

    private void sendMessageOfContactToAdmin(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("""
                \uD83D\uDCDE <b>Biz bilan aloqa</b>\s
                
                ✍️ Savollaringiz yoki takliflaringiz bo‘lsa, shu yerga yozing. \s""");

        sendMessage.setParseMode("HTML");

        cancelButton(sendMessage);

        user.setUserStep(UserStep.WAITING_FOR_MESSAGE);

        myExecute(sendMessage);

    }

    private void sendMessageOfGuide(Long chatId) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("""
                \uD83D\uDCD6 <b>Botdan foydalanish bo‘yicha yo‘riqnoma</b>
                
                \uD83D\uDD39 Botdan qanday foydalanish haqida batafsil qo‘llanma tayyorladik. \s
                \uD83D\uDD39 Bu yerda barcha funksiyalar haqida tushuntirilgan. \s
                
                \uD83D\uDCCC Quyidagi tugma orqali yo'riqnoma bilan tanishing:
                """);

        sendMessage.setParseMode("HTML");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton guideButton = new InlineKeyboardButton();
        guideButton.setText("📖 Yo‘riqnoma bilan tanishish");
        guideButton.setUrl("https://telegra.ph/Som-Valyuta-Bot-Yoriqnomasi-02-21");

        List<InlineKeyboardButton> row = List.of(guideButton);
        inlineKeyboardMarkup.setKeyboard(List.of(row));

        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        myExecute(sendMessage);

    }

    private void sendMessageOfPremium(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("""
                \uD83D\uDC51 <b>Premium xizmatlar hozircha mavjud emas!</b>
                
                ⚡ Agar bot sizga yoqqan bo‘lsa va dasturchini qo‘llab-quvvatlamoqchi bo‘lsangiz, quyidagi karta raqamlariga xayriya qilishingiz mumkin: \s
                
                \uD83D\uDCB3 <b>Donat uchun karta raqamlar:</b> \s
                \uD83D\uDCB0 Click / PayMe / Uzum: <code>8600 0504 5884 0594</code> \s
                
                Sizning yordamingiz botni yanada rivojlantirishga hissa qo‘shadi! \uD83D\uDE0A \s
                """);

        sendMessage.setParseMode("HTML");

        menuKeyboard(sendMessage, user);

        myExecute(sendMessage);

    }

    private void senMessageOfAdd(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("📌 Sevimli valyutalaringizni tanlang: \n\n" +
                "Maximum <b>" + (user.getCurrencySize()) + "</b> ta valyuta tanlay olasiz !");

        sendMessage.setParseMode("HTML");

        List<String> userAllCurrency = user.getAllCurrency();

        user.setCurrentPage(0);

        InlineKeyboardMarkup currencySelectionKeyboard = getCurrencySelectionKeyboard(user.getCurrentPage(), userAllCurrency);

        sendMessage.setReplyMarkup(currencySelectionKeyboard);

        myExecute(sendMessage);

    }

    public InlineKeyboardMarkup getCurrencySelectionKeyboard(int currentPage, List<String> selectedCurrencies) {
        int pageSize = 9;
        List<String> allCurrencies = getAllCurrencies().stream().filter(str -> !str.equals("UZS")).toList();

        int totalPages = (int) Math.ceil((double) allCurrencies.size() / pageSize);
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allCurrencies.size());

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = startIndex; i < endIndex; i += 3) { // 3 tadan joylash

            List<InlineKeyboardButton> row = new ArrayList<>();

            int j = 0;

            while (j < 3 && i + j < endIndex) {

                String currency = allCurrencies.get(i + j);

                boolean isSelected = selectedCurrencies.contains(currency);

                InlineKeyboardButton button = new InlineKeyboardButton();

                button.setText((isSelected ? "✅ " : "▫️ ") + currency + getFlag(currency));

                button.setCallbackData("toggle_" + currency);

                row.add(button);

                j++;
            }
            keyboard.add(row);
        }


        List<InlineKeyboardButton> navButtons = new ArrayList<>();
        if (currentPage > 0) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("◀️ Oldingi");
            prevButton.setCallbackData("prevPage");
            navButtons.add(prevButton);
        }
        if (currentPage < totalPages - 1) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("Keyingi ▶️");
            nextButton.setCallbackData("nextPage");
            navButtons.add(nextButton);
        }
        if (!navButtons.isEmpty()) {
            keyboard.add(navButtons);
        }

        List<InlineKeyboardButton> actionButtons = new ArrayList<>();

        InlineKeyboardButton save = new InlineKeyboardButton("🔄 Saqlash");
        save.setCallbackData("saveSelection");
        InlineKeyboardButton clear = new InlineKeyboardButton("❌ Tozalash");
        clear.setCallbackData("clearSelection");
        actionButtons.add(save);
        actionButtons.add(clear);

        keyboard.add(actionButtons);

        return new InlineKeyboardMarkup(keyboard);
    }


    private void backMessage(Message message, User user) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Bosh sahifa");
        menuKeyboard(sendMessage, user);

        myExecute(sendMessage);
    }

    private void sendMessageForFavoriteCurrenciesInSeeCurrencies(Long chatId, User user) {

        List<String> userAllCurrency = user.getAllCurrency();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());

        if (user.getAllCurrency().size() == 1) {

            sendMessage.setText("""
                    ❌ Siz hali hech qanday valyuta tanlamagansiz.\s
                    
                    ➕ <b>Qo'shish</b> tugmasini bosing va valyutalar qo‘shing.""");

            sendMessage.setParseMode("HTML");

        } else {

            StringBuilder stringBuilder = new StringBuilder().append("\uD83D\uDCCC <b>Siz tanlagan valyutalar: </b>\n\n");

            List<String> list = userAllCurrency.stream().filter(curr -> !curr.equals("UZS")).toList();

            for (String string : list) {

                stringBuilder.append("✅").append(" ").append(string).append(" ").append(getFlag(string)).
                        append(" ( ").append(actions.getCurrencyNameUz(string)).append(" )")
                        .append("\n");

            }

            sendMessage.setText(stringBuilder.toString());

        }

        sendMessage.setParseMode("HTML");
        sendMessage.setReplyMarkup(replyKeyboardForFavoriteCurrenciesMenu());

        myExecute(sendMessage);

    }

    private void sendMessageForFavoriteCurrencies(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("""
                \uD83D\uDCCC Sevimli valyutalar
                
                Bu bo‘limda tez-tez ishlatadigan valyutalaringizni tanlashingiz mumkin. \
                Tanlangan valyutalar asosiy menyuda ko‘rinadi va konvertatsiya qilish qulay bo‘ladi.
                
                ✅ <b>Tanlangan | ▫️ Tanlanmagan</b>""");

        sendMessage.setParseMode("HTML");

        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardForFavoriteCurrenciesMenu();

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        myExecute(sendMessage);

        user.setUserStep(UserStep.IN_FAVORITE_CURRENCIES_MENU);

    }

    private ReplyKeyboardMarkup replyKeyboardForFavoriteCurrenciesMenu() {

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("➕ Qo'shish");
        firstRow.add("\uD83D\uDC41 Ko'rish");

        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("\uD83D\uDD19 Ortga qaytish");
        secondRow.add("\uD83D\uDD1D Asosiy menyuga");

        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;

    }

    private void sendMessageOfSettings(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("""
                Hurmatli foydalanuvchi! \uD83D\uDE0A\s
                Ushbu bo‘limda siz <b>valyuta konvertatsiya botining sozlamalarini o‘zgartirishingiz</b> mumkin.
                
                \uD83D\uDCCC <b>Sevimli valyutalar</b>  – Faqat sizga kerakli valyutalarni qo‘shing yoki o‘chiring.
                \uD83D\uDD19 <b>Ortga qaytish</b> – Asosiy menyuga qaytish.""");

        sendMessage.setParseMode("HTML");

        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardForSettingsMenu();

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        myExecute(sendMessage);

        user.setUserStep(UserStep.IN_SETTINGS_MENU);

    }

    private ReplyKeyboardMarkup replyKeyboardForSettingsMenu() {

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstButton = new KeyboardRow();
        firstButton.add("\uD83D\uDCCC Sevimli valyutalar");

        KeyboardRow secondButton = new KeyboardRow();
        secondButton.add("\uD83D\uDD19 Ortga qaytish");

        keyboardRows.add(firstButton);
        keyboardRows.add(secondButton);

        replyKeyboardMarkup.setKeyboard(keyboardRows);

        return replyKeyboardMarkup;

    }

    private void sendMessageOfCalculate(Message message, User user) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());

        sendMessage.setText("""
                \uD83D\uDCB1 <b>Valyuta konvertori</b> \s
                
                \uD83D\uDCCC Bu yerda valyutalarni oson almashtirishingiz mumkin: \s
                \uD83D\uDD39 <b>USD → UZS</b> (Dollar → So‘m) \s
                \uD83D\uDD39 <b>UZS → USD</b> (So‘m → Dollar) \s
                
                \uD83D\uDD3D <b>Konvertatsiya qilmoqchi bo‘lgan valyutalaringizni tanlang</b> va <b>Saqlash ✅</b> tugmasini bosing!
                """);

        sendMessage.setParseMode("HTML");

        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardForCalculateMenu(user);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        myExecute(sendMessage);
    }

    private InlineKeyboardMarkup inlineKeyboardForCalculateMenu(User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String firstCur = user.getSelectedCurrency()[0];

        String firstCurFlag = getFlag(firstCur);

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText(firstCurFlag + firstCur);
        firstButton.setCallbackData("convert-first-menu");

        InlineKeyboardButton convertSwap = new InlineKeyboardButton();
        convertSwap.setText("\uD83D\uDD04");
        convertSwap.setCallbackData("convert-swap");

        InlineKeyboardButton secondButton = new InlineKeyboardButton();

        String secondCur = user.getSelectedCurrency()[1];
        String secondCurFlag = getFlag(secondCur);

        secondButton.setText(secondCurFlag + secondCur);
        secondButton.setCallbackData("convert-second-menu");

        InlineKeyboardButton thirdButton = new InlineKeyboardButton();
        thirdButton.setText("Saqlash ✅");
        thirdButton.setCallbackData("convert-save");

        rows.add(List.of(firstButton, convertSwap, secondButton));
        rows.add(List.of(thirdButton));
        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    private void sendAlreadyTimeMessage(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("Sizda <b>🔔 Qo‘ng‘iroqcha</b> xizmati faollashtirilgan !\n\n" +
                "Faollashtirilgan vaqt : " + "<b>" + user.getBallTime().getTime() + "</b>\n\n" +
                "\uD83D\uDD54 Vaqtni o'zgartirmoqchi yoki Obunani o'chirmoqchi bo'lsangiz pastdagi tugmalardan foydalaning \uD83D\uDC47");

        sendMessage.setParseMode("HTML");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("O'zgartirish");
        yesButton.setCallbackData("ball-time-change");

        InlineKeyboardButton offButton = new InlineKeyboardButton();
        offButton.setText("O'chirish");
        offButton.setCallbackData("ball-time-off");


        buttons.add(List.of(yesButton, offButton));
        inlineKeyboardMarkup.setKeyboard(buttons);

        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        myExecute(sendMessage);

    }

    private void sendMessageScheduleTime(User user, Message message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime targetTime = LocalTime.parse(user.getBallTime().getTime(), formatter);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetDateTime = now.with(targetTime);

        long initialDelay = now.isAfter(targetDateTime) ?
                Duration.between(now, targetDateTime.plusDays(1)).getSeconds() :
                Duration.between(now, targetDateTime).getSeconds();

        // Eski taskni to‘xtatamiz (agar bor bo‘lsa)
        if (user.getUserTask() != null) {
            user.getUserTask().cancel(true);
        }

        // Yangi task yaratamiz va foydalanuvchiga bog‘laymiz
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(
                () -> sendMessageOfCurrencies(message.getChatId(), user),
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );

        user.setUserTask(scheduledTask); // User objectiga yangi taskni bog‘lash

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("✅ Sizning vaqtingiz: " + user.getBallTime().getTime() + " ga belgilandi");
        menuKeyboard(sendMessage, user);
        myExecute(sendMessage);
    }


    private void sendTimeSelectionKeyboard(Long chatId) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("""
                \uD83D\uDD14 <b>Qo‘ng‘iroqcha faollashtirilmoqda!</b> \s
                
                Bu xizmat orqali siz har kuni belgilangan vaqtda <b>eng so‘nggi valyuta kurslarini</b> olasiz. \s
                
                \uD83D\uDCCC <b>Qanday ishlaydi?</b> \s
                - O‘zingizga qulay vaqtni tanlang. \s
                - Har kuni <b>3 ta asosiy valyuta kursi</b> avtomatik yuboriladi: \s
                  ✅ <b>USD/UZS</b> (Dollar → So‘m) \s
                  ✅ <b>EUR/UZS</b> (Yevro → So‘m) \s
                  ✅ <b>RUB/UZS</b> (Rubl → So‘m) \s
                
                ⏰ <b>Vaqtni tanlang va kurslarni doimiy ravishda qabul qiling!</b> \uD83C\uDF1F
                """);

        sendMessage.setParseMode("HTML");

        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardForTimeSelection();

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        myExecute(sendMessage);

    }

    private ReplyKeyboardMarkup replyKeyboardForTimeSelection() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("06:00");
        row1.add("08:00");
        row1.add("09:00");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("10:00");
        row2.add("12:00");
        row2.add("15:00");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("18:00");
        row3.add("20:00");
        row3.add("22:00");

        keyboardMarkup.setKeyboard(Arrays.asList(row1, row2, row3));
        return keyboardMarkup;

    }

    private void sendMessageOfCurrencies(Long chatId, User user) {

        if (user.getAllCurrency().size() != 1) {

            actions = new Actions();

            String stringBuilder = getCurrenciesButtons(user);

            String currencyDate = actions.getCurrencyDate();

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId.toString());
            sendMessage.setText("💰 <b>" + currencyDate + " sanasi uchun valyuta kurslari:</b>\n\n" +
                    stringBuilder + "\n" +
                    "\uD83C\uDFE6 Kurslar Markaziy bank ma’lumotlariga asoslangan.");
            sendMessage.setParseMode("HTML");

            sendMessage.setReplyMarkup(inlineKeyboardForCurrencyMessage(user));

            myExecute(sendMessage);


        } else {

            SendMessage sendMessage = new SendMessage();

            sendMessage.setChatId(chatId);

            sendMessage.setText("""
                    <b> ⚠️ Sevimli valyutalar topilmadi! </b>\s
                    
                    Siz hali hech qanday valyutani tanlamagansiz yoki ularni o‘chirib tashlagansiz.\s
                    
                    ✅ Valyutalarni qo‘shish uchun <b> "⚙️ Sozlamalar " \uD83D\uDD1C "\uD83D\uDCCC Sevimli valyutalar " </b>""");

            sendMessage.setParseMode("HTML");

            myExecute(sendMessage);

        }


    }

    private String getCurrenciesButtons(User user) {
        StringBuilder stringBuilder = new StringBuilder();

        List<String> userAllCurrency = user.getAllCurrency();

        Map<String, Double> currenciesMap = new HashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(userAllCurrency.size());

        Map<String, Future<Double>> futureCurrenciesMap = new HashMap<>();

        try {

            for (String string : userAllCurrency) {

                if (!string.equals("UZS"))
                    futureCurrenciesMap.put(string, executorService.submit(() -> actions.getCurrencyRate(string)));

            }

            for (Map.Entry<String, Future<Double>> entry : futureCurrenciesMap.entrySet()) {
                currenciesMap.put(entry.getKey(), entry.getValue().get());
            }

            for (Map.Entry<String, Double> entry : currenciesMap.entrySet()) {
                String flag = getFlag(entry.getKey());
                String ccyNmUz = actions.getCurrencyNameUz(entry.getKey());

                if (flag != null) {

                    stringBuilder.append(flag).append(entry.getKey().equals("IRR") ? 10 : 1).append(" <b>").
                            append(entry.getKey()).append("</b> ").append(ccyNmUz).append(" = ").append("<b>").
                            append(rateDoubleToString(entry.getValue())).append("</b>").append(" UZS").append('\n');

                }
            }

            return stringBuilder.toString();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }

    }

    private InlineKeyboardMarkup inlineKeyboardForCurrencyMessage(User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("Yangilash \uD83D\uDD04");
        firstButton.setCallbackData("update");

        InlineKeyboardButton secondButton = new InlineKeyboardButton();
        secondButton.setText("Ulashish ↗️");

        String currencyDate = actions.getCurrencyDate();
        String text = "💰 " + currencyDate + " sanasi uchun valyuta kurslari:\n\n" +
                getCurrenciesButtons(user).replaceAll("<[^>]*>", "") + "\n" +
                "\uD83C\uDF0D @SomValyutaBot orqali eng so‘nggi valyuta kurslarini kuzatib boring";

        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

        String shareUrl = "https://t.me/share/url?url=" + encodedText;

        secondButton.setUrl(shareUrl);

        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        keyboardButtons.add(List.of(firstButton, secondButton));

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return inlineKeyboardMarkup;
    }

    private String rateDoubleToString(double value) {
        int integerPart = (int) value;
        double fractionPart = value - integerPart;
        int division = 1000;

        StringBuilder string = new StringBuilder();

        while (integerPart > 0) {
            int currentBlock = integerPart % division;
            integerPart /= division;

            if (integerPart > 0) {
                string.insert(0, String.format("%03d ", currentBlock)); // 3 xonali formatda chiqarish
            } else {
                string.insert(0, currentBlock + " "); // Eng oldingi blokni oddiy qo'shish
            }
        }

        if (string.isEmpty()) {
            string.append("0");
        }

        String fractionString = String.format("%.2f", fractionPart).substring(2);
        return string.toString().trim() + "." + fractionString;
    }


    private String getFlag(String key) {

        Path filePath = FileUtil.getResourceFilePath("flags/flags.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                int indexOf = line.indexOf('|');

                if (indexOf != -1) {
                    String substring = line.substring(indexOf + 1);

                    if (line.contains(key)) {
                        return substring;
                    }
                }
            }

        } catch (IOException e) {
            BotLogger.logError("Reading flags file failed", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private List<String> getAllCurrencies() {
        List<String> allCurrencies = new ArrayList<>();
//        String filePath = "D:\\Project's\\JavaProject's\\som-valyuta-bot\\v2.0\\som-valyuta-bot\\src\\main\\resources\\flags\\flags.txt";
        Path filePath = FileUtil.getResourceFilePath("flags/flags.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                int indexOf = line.indexOf('|');

                if (indexOf != -1) {

                    String substring = line.substring(0, indexOf);

                    allCurrencies.add(substring);

                }

            }

        } catch (IOException e) {
            BotLogger.logError("Reading flags file failed for getAllCurrencies", e);
            throw new RuntimeException(e);
        }

        return allCurrencies;

    }

    private void sendMessageOfStart(Long chatId, User user) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());

        sendMessage.setText("""
                🤖 <b>Assalomu alaykum!</b>
                
                Sizga eng so‘nggi valyuta kurslarini tez va oson yetkazib beruvchi botga xush kelibsiz! \uD83D\uDCB0
                
                📌 <b>Bu bot sizga qanday yordam beradi?</b>
                \uD83D\uDD39 <b>Valyuta kurslari</b> – Markaziy bankning eng so‘nggi ma’lumotlari \uD83D\uDCC9
                \uD83D\uDD39 <b>Hisoblash</b> – Valyutalar o‘rtasida tezkor konvertatsiya \uD83D\uDCB1
                \uD83D\uDD39 <b>Obuna</b> – Kunlik kurslarni avtomatik qabul qilish \uD83D\uDCE9
                \uD83D\uDD39 <b>Aloqa</b> – Taklif va savollaringiz uchun biz bilan bog‘laning \uD83D\uDCDE\s
                
                👉 <b>Pastdagi tugmalardan birini bosing va foydalanishni boshlang!</b> 🚀""");

        sendMessage.setParseMode("HTML");

        menuKeyboard(sendMessage, user);

        myExecute(sendMessage);
    }

    private void menuKeyboard(SendMessage sendMessage, User user) {

        if (!user.isPremiumUser()) {

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setSelective(true);

            List<KeyboardRow> rowList = new ArrayList<>();

            KeyboardRow firstRow = new KeyboardRow();
            firstRow.add("\uD83D\uDCB0 Kurs");
            firstRow.add("\uD83D\uDD14 Qo‘ng‘iroqcha");

            KeyboardRow secondRow = new KeyboardRow();
            secondRow.add("\uD83D\uDD22 Hisoblash");
            secondRow.add("⚙️ Sozlamalar");

            KeyboardRow thirdRow = new KeyboardRow();
            thirdRow.add("\uD83D\uDC51 Premium");
            thirdRow.add("\uD83D\uDCD6 Yo'riqnoma");

            KeyboardRow fourRow = new KeyboardRow();
            fourRow.add("\uD83D\uDCDE Biz bilan aloqa");

            rowList.add(firstRow);
            rowList.add(secondRow);
            rowList.add(thirdRow);
            rowList.add(fourRow);

            replyKeyboardMarkup.setKeyboard(rowList);

            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            user.setUserStep(UserStep.SELECT_MENU);
        }
    }

    private User checkedUser(Message message) {

        if (!users.isEmpty()) {

            for (User user : users) {
                if (user.getUserId().equals(message.getChatId())) {
                    return user;
                }
            }
        }

        Long chatId = message.getChatId();
        String firstName = message.getChat().getFirstName();
        String lastName = message.getChat().getLastName();
        String userName = message.getChat().getUserName();

        List<String> simpleCurrency = new ArrayList<>();
        simpleCurrency.add("USD");
        simpleCurrency.add("EUR");
        simpleCurrency.add("RUB");
        simpleCurrency.add("UZS");

        BallTime ballTime = new BallTime("08:00", false);

        int currencySize = 9 - simpleCurrency.size();

        ScheduledFuture<?> userTask = null;

        User user = new User(chatId, firstName, lastName, userName, UserStep.START, simpleCurrency, ballTime, currencySize,
                new String[]{"USD", "UZS"}, 0, userTask, false);

        users.add(user);

        saveUsersToFile();

        return user;

    }

    private User getUser(Long chatId) {

        for (User user : users) {
            if (user.getUserId().equals(chatId)) {
                return user;
            }
        }

        return null;

    }

    public static void saveUsersToFile() {
        try (FileWriter writer = new FileWriter(FILE_PATH, false)) {
            gson.toJson(users, writer);
            System.out.println("Users saved successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void myExecute(SendMessage sendMessage) {

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }
}
