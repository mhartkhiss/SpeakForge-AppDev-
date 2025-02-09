package com.example.appdev.utils;

import android.os.CountDownTimer;
import java.util.HashMap;

public class PasswordResetTimerManager {
    private static PasswordResetTimerManager instance;
    private HashMap<String, TimerInfo> timerMap = new HashMap<>();
    
    private PasswordResetTimerManager() {}
    
    public static synchronized PasswordResetTimerManager getInstance() {
        if (instance == null) {
            instance = new PasswordResetTimerManager();
        }
        return instance;
    }
    
    public static class TimerInfo {
        public long remainingTime;
        public CountDownTimer timer;
        public TimerCallback callback;
        
        public TimerInfo(long remainingTime, CountDownTimer timer, TimerCallback callback) {
            this.remainingTime = remainingTime;
            this.timer = timer;
            this.callback = callback;
        }
    }
    
    public interface TimerCallback {
        void onTick(long millisUntilFinished);
        void onFinish();
    }
    
    public void startTimer(String email, long duration, TimerCallback callback) {
        // Check if timer already exists
        if (timerMap.containsKey(email)) {
            TimerInfo existingTimer = timerMap.get(email);
            if (existingTimer != null && existingTimer.remainingTime > 0) {
                // Resume existing timer
                startNewTimer(email, existingTimer.remainingTime, callback);
                return;
            }
        }
        
        // Start new timer
        startNewTimer(email, duration, callback);
    }
    
    private void startNewTimer(String email, long duration, TimerCallback callback) {
        // Cancel existing timer if any
        cancelTimer(email);
        
        CountDownTimer timer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (timerMap.containsKey(email)) {
                    timerMap.get(email).remainingTime = millisUntilFinished;
                }
                callback.onTick(millisUntilFinished);
            }
            
            @Override
            public void onFinish() {
                timerMap.remove(email);
                callback.onFinish();
            }
        };
        
        timerMap.put(email, new TimerInfo(duration, timer, callback));
        timer.start();
    }
    
    public void cancelTimer(String email) {
        TimerInfo timerInfo = timerMap.get(email);
        if (timerInfo != null && timerInfo.timer != null) {
            timerInfo.timer.cancel();
        }
        timerMap.remove(email);
    }
    
    public boolean hasActiveTimer(String email) {
        TimerInfo timerInfo = timerMap.get(email);
        return timerInfo != null && timerInfo.remainingTime > 0;
    }
    
    public long getRemainingTime(String email) {
        TimerInfo timerInfo = timerMap.get(email);
        return timerInfo != null ? timerInfo.remainingTime : 0;
    }
} 