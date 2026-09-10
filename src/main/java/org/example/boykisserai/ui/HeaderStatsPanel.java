package org.example.boykisserai.ui;

import org.example.boykisserai.domain.constant.EmotionType;
import org.example.boykisserai.provider.speech.SpeechState; // <-- УНИВЕРСАЛЬНЫЙ ФЛАГ РЕЧИ
import org.example.boykisserai.service.state.DashboardStateService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

import static org.example.boykisserai.ui.DashboardGuiFrame.*;

public class HeaderStatsPanel extends JPanel {

    private final DashboardStateService stateService;
    private final Map<EmotionType, Color> emotionColors = new EnumMap<>(EmotionType.class);

    private final JLabel statusDot = new JLabel("● Слушает");
    private final JLabel emotionBadge = new JLabel("РАДОСТЬ");
    private final JLabel statTrustValue = new JLabel("100%");
    private final JLabel statMessagesValue = new JLabel("0");
    private final JLabel statUptimeValue = new JLabel("00:00:00");

    public HeaderStatsPanel(DashboardStateService stateService) {
        this.stateService = stateService;
        initColors();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_APP);

        add(buildHeaderRow());
        add(Box.createVerticalStrut(8));
        add(buildStatsStrip());
    }

    private void initColors() {
        emotionColors.put(EmotionType.JOY, new Color(245, 158, 11));
        emotionColors.put(EmotionType.AFFECTION, new Color(236, 72, 153));
        emotionColors.put(EmotionType.TRUST, ACCENT_GREEN);
        emotionColors.put(EmotionType.INTEREST, ACCENT_BLURPLE);
        emotionColors.put(EmotionType.SURPRISE, new Color(168, 85, 247));
        emotionColors.put(EmotionType.IRRITATION, ACCENT_RED);
        emotionColors.put(EmotionType.SADNESS, new Color(100, 116, 139));
        emotionColors.put(EmotionType.FATIGUE, new Color(71, 85, 105));
    }

    private JPanel buildHeaderRow() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(BG_CARD);

        JLabel logo = new JLabel("● Boykisser AI");
        logo.setFont(FONT_HEADER);
        logo.setForeground(ACCENT_BLURPLE);

        statusDot.setFont(FONT_MONO);
        statusDot.setForeground(ACCENT_GREEN);

        left.add(logo);
        left.add(Box.createHorizontalStrut(8));
        left.add(statusDot);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(BG_CARD);
        emotionBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emotionBadge.setOpaque(true);
        emotionBadge.setForeground(Color.WHITE);
        emotionBadge.setBackground(emotionColors.get(EmotionType.JOY));
        emotionBadge.setBorder(new EmptyBorder(4, 10, 4, 10));

        right.add(new JLabel("Настроение:"));
        right.add(emotionBadge);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatsStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 3, 8, 0));
        strip.setBackground(BG_APP);
        strip.add(createStatCard("ДОВЕРИЕ", statTrustValue, ACCENT_GREEN));
        strip.add(createStatCard("СООБЩЕНИЙ ЗА СЕССИЮ", statMessagesValue, ACCENT_BLURPLE));
        strip.add(createStatCard("ВРЕМЯ РАБОТЫ", statUptimeValue, new Color(245, 158, 11)));
        return strip;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 10));
        titleLabel.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(accent);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        statTrustValue.setText(stateService.getTrustLevel() + "%");
        statMessagesValue.setText(String.valueOf(stateService.getTotalMessageCount()));
        statUptimeValue.setText(formatUptime(stateService.getSessionStartTime()));

        EmotionType emotion = stateService.getCurrentEmotion();
        if (emotion != null) {
            emotionBadge.setText(emotion.name());
            emotionBadge.setBackground(emotionColors.getOrDefault(emotion, ACCENT_BLURPLE));
        }

        // ПРОВЕРЯЕМ ЕДИНЫЙ ФЛАГ SPEECHSTATE
        if (SpeechState.isSpeaking) {
            statusDot.setText("● Озвучивает...");
            statusDot.setForeground(new Color(236, 72, 153));
        } else {
            statusDot.setText("● Слушает");
            statusDot.setForeground(ACCENT_GREEN);
        }
    }

    private String formatUptime(long startMillis) {
        long sec = (System.currentTimeMillis() - startMillis) / 1000;
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }
}