# 📌 **So'mValyuta Bot v2.0**

**So'mValyuta** is a Telegram bot that automatically updates currency exchange rates using the Central Bank API. This bot fetches the latest currency rates daily and provides users with an easy-to-use interface.

---

## ✨ **Features**
- ✅ **Integration with the Central Bank API**
- ✅ **Automatic daily currency rate updates**
- ✅ **Easy interaction via Telegram bot**
- ✅ **Secure token management with `.env` file**
- ✅ **Platform-independent resources (e.g., `flags.txt` for country flags)**

---

## 📌 **Installation & Setup**

### **1️⃣ Clone the Repository**
```bash
git clone https://github.com/MuminjonovAsrorbek/Som-Valyuta-bot.git 
cd som-valyuta  
```
### **2️⃣ Install Dependencies**
If using Maven:
```bash
mvn install
```
### **3️⃣ Configure the .env File**
Create a .env file in the root directory and add the following details:
```ini
BOT_TOKEN=your-telegram-bot-token
API_URL=https://cbu.uz/uz/arkhiv-kursov-valyut/json/
```
📌 Note: The .gitignore file includes .env, so it won’t be uploaded to GitHub.
### **4️⃣ Run the Bot**
```bash
java -jar som-valyuta-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
```
If using **IntelliJ IDEA** or **VS Code**, you can run the `main()` method from `Application.java`.

---

## **🛠 Technologies**

- Java 17+
- Telegram Bot API
- Central Bank API
- dotenv-java (for secure environment variables)

---

## 🤝 **Contributing**

If you would like to improve this bot:

  1.Open an issue with any bugs or feature suggestions
  
  2.Fork the repository, make improvements, and submit a pull request

---

## 📜 **License**
This project is licensed under the **MIT** License.
