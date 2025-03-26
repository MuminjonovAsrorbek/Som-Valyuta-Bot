package org.example.userData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    private Long userId;

    private String firstName;

    private String lastName;

    private String username;

    private UserStep userStep;

    private List<String> allCurrency;

    private BallTime ballTime;

    private int currencySize;

    private String[] selectedCurrency;

    private int currentPage;

    private ScheduledFuture<?> userTask;

    private boolean isPremiumUser;

}
