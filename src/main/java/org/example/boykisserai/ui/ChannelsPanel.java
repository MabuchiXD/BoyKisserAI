package org.example.boykisserai.ui;

import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.state.DashboardStateService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.example.boykisserai.ui.DashboardGuiFrame.*;

public class ChannelsPanel extends JPanel {

    private final DashboardStateService stateService;
    private final CharacterService characterService;

    private final JTextArea localChatArea = new JTextArea();
    private final JTextArea discordChatArea = new JTextArea();
    private final JTextArea twitchChatArea = new JTextArea();
    private final JTextArea impactChatArea = new JTextArea();

    // МНОГОСТРОЧНОЕ ПОЛЕ ВВОДА (КАК В DISCORD / TELEGRAM)
    private final JTextArea chatInputField = new JTextArea(2, 20);
    private final JButton btnSend = new JButton("Отправить");

    private int lastLogCount = 0;

    public ChannelsPanel(DashboardStateService stateService, CharacterService characterService) {
        this.stateService = stateService;
        this.characterService = characterService;

        setLayout(new BorderLayout());
        setBackground(BG_CARD);
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));

        add(buildTabs(), BorderLayout.CENTER);
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_TITLE);

        setupChatArea(localChatArea);
        setupChatArea(discordChatArea);
        setupChatArea(twitchChatArea);
        setupChatArea(impactChatArea);

        // Вкладка 1: Локальный чат с полем ввода
        JPanel localTab = new JPanel(new BorderLayout(0, 8));
        localTab.setBackground(BG_CARD);
        localTab.add(createScroll(localChatArea), BorderLayout.CENTER);
        localTab.add(buildInputPanel(), BorderLayout.SOUTH);

        tabbedPane.addTab("# локальный", localTab);
        tabbedPane.addTab("# discord", createScroll(discordChatArea));
        tabbedPane.addTab("# twitch", createScroll(twitchChatArea));
        tabbedPane.addTab("# нейро-анализ", createScroll(impactChatArea));

        return tabbedPane;
    }

    private JPanel buildInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(BG_CARD);
        inputPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        // Настройка многострочного поля ввода
        chatInputField.setFont(FONT_BODY);
        chatInputField.setLineWrap(true);
        chatInputField.setWrapStyleWord(true);
        chatInputField.setBackground(BG_ELEVATED);
        chatInputField.setForeground(TEXT_PRIMARY);
        chatInputField.setMargin(new Insets(6, 10, 6, 10));
        chatInputField.putClientProperty("JTextArea.placeholderText", "Напишите сообщение... (Enter — отправить, Shift+Enter — новая строка)");

        // ================= НАСТРОЙКА SHIFT+ENTER И ENTER =================
        // 1. Shift + Enter -> вставляет перенос строки \n
        KeyStroke shiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        chatInputField.getInputMap().put(shiftEnter, "insert-break");

        // 2. Обычный Enter -> отправляет сообщение в чат
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        chatInputField.getInputMap().put(enter, "send-message");
        chatInputField.getActionMap().put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendTextMessage();
            }
        });

        JScrollPane inputScroll = new JScrollPane(chatInputField);
        inputScroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        inputScroll.setPreferredSize(new Dimension(300, 46));

        btnSend.setPreferredSize(new Dimension(105, 46));
        btnSend.setBackground(ACCENT_BLURPLE);
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(FONT_TITLE);
        btnSend.addActionListener(e -> sendTextMessage());

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);
        return inputPanel;
    }

    private JScrollPane createScroll(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        return scroll;
    }

    private void setupChatArea(JTextArea area) {
        area.setEditable(false);
        area.setFont(FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(BG_APP);
        area.setForeground(TEXT_PRIMARY);
        area.setMargin(new Insets(10, 12, 10, 12));
    }

    private void sendTextMessage() {
        String text = chatInputField.getText().trim();
        if (text.isEmpty()) return;

        chatInputField.setText("");
        btnSend.setEnabled(false);

        CompletableFuture.runAsync(() -> {
            try {
                characterService.chat("mabuchi", "Мабучи", "LOCAL", text);
            } finally {
                SwingUtilities.invokeLater(() -> btnSend.setEnabled(true));
            }
        });
    }

    private String cleanEmoji(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "").trim();
    }

    public void refresh() {
        List<DashboardStateService.ChatMessageItem> allMessages = stateService.getAllMessagesCopy();
        if (allMessages.size() == lastLogCount) return;

        // 1. Локальный чат
        StringBuilder sbLocal = new StringBuilder();
        for (var m : stateService.getLocalMessages()) {
            if (m.isAi()) {
                sbLocal.append(String.format("[%s] Boykisser (%s):\n   \"%s\"\n\n", m.timestamp(), m.mood(), cleanEmoji(m.text())));
            } else {
                sbLocal.append(String.format("[%s] %s:\n   %s\n\n", m.timestamp(), m.sender(), cleanEmoji(m.text())));
            }
        }
        localChatArea.setText(sbLocal.toString());
        localChatArea.setCaretPosition(localChatArea.getDocument().getLength());

        // 2. Discord чат
        StringBuilder sbDiscord = new StringBuilder();
        for (var m : stateService.getDiscordMessages()) {
            if (m.isAi()) {
                sbDiscord.append(String.format("[%s] Boykisser (%s):\n   \"%s\"\n\n", m.timestamp(), m.mood(), cleanEmoji(m.text())));
            } else {
                sbDiscord.append(String.format("[%s] Discord | %s:\n   %s\n\n", m.timestamp(), m.sender(), cleanEmoji(m.text())));
            }
        }
        discordChatArea.setText(sbDiscord.toString());
        discordChatArea.setCaretPosition(discordChatArea.getDocument().getLength());

        // 3. Twitch чат
        StringBuilder sbTwitch = new StringBuilder();
        for (var m : stateService.getTwitchMessages()) {
            if (m.isAi()) {
                sbTwitch.append(String.format("[%s] Boykisser (%s):\n   \"%s\"\n\n", m.timestamp(), m.mood(), cleanEmoji(m.text())));
            } else {
                sbTwitch.append(String.format("[%s] Twitch | %s:\n   %s\n\n", m.timestamp(), m.sender(), cleanEmoji(m.text())));
            }
        }
        if (stateService.getTwitchMessages().isEmpty()) {
            twitchChatArea.setText("● Ожидание сообщений от зрителей Twitch...\n");
        } else {
            twitchChatArea.setText(sbTwitch.toString());
            twitchChatArea.setCaretPosition(twitchChatArea.getDocument().getLength());
        }

        StringBuilder sbImpact = new StringBuilder();
        for (var m : allMessages) {
            if ("SYSTEM".equalsIgnoreCase(m.platform())) {
                // Служебные уведомления базы данных выводятся аккуратной плашкой:
                sbImpact.append(String.format("[%s] 💾 БАЗА ДАННЫХ:\n   %s\n\n", m.timestamp(), m.text()));
            } else if (m.isAi()) {
                sbImpact.append(String.format("[%s] Boykisser: \"%s\"\n", m.timestamp(), cleanEmoji(m.text())));

                String actionDesc = switch (m.action()) {
                    case "SLEEP" -> "СОН (Спящий режим)";
                    case "WAKE" -> "ПРОБУЖДЕНИЕ (Выход из сна)";
                    default -> "ДИАЛОГ";
                };

                sbImpact.append(String.format("   -> Режим:    [%s]\n", actionDesc));
                sbImpact.append(String.format("   -> Эмоция:   [%s]\n", m.mood()));
                sbImpact.append(String.format("   -> Доверие:  [%+d]\n", m.trustDelta()));
                sbImpact.append(String.format("   -> Причина:  %s\n\n", m.reason().isBlank() ? "Обычный диалог" : m.reason()));
            } else {
                sbImpact.append(String.format("[%s] [%s] %s:\n   \"%s\"\n", m.timestamp(), m.platform(), m.sender(), cleanEmoji(m.text())));
            }
        }
        impactChatArea.setText(sbImpact.toString());
        impactChatArea.setCaretPosition(impactChatArea.getDocument().getLength());

        lastLogCount = allMessages.size();
    }
}