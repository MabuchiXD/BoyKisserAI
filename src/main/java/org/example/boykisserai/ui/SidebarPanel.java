package org.example.boykisserai.ui;

import org.example.boykisserai.domain.constant.EmotionType;
import org.example.boykisserai.domain.constant.PersonalityPhase;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.service.memory.MemoryService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.example.boykisserai.service.state.EmotionService;
import org.example.boykisserai.service.relationship.RelationshipService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.example.boykisserai.ui.DashboardGuiFrame.*;

public class SidebarPanel extends JPanel {

    private final DashboardStateService stateService;
    private final EmotionService emotionService;
    private final RelationshipService relationshipService;
    private final MemoryService memoryService;

    private final Map<EmotionType, String> emotionLabels = new EnumMap<>(EmotionType.class);
    private final Map<EmotionType, JButton> moodButtons = new EnumMap<>(EmotionType.class);

    private final JLabel speakerLabel = new JLabel("Мабучи");
    private final JLabel platformBadge = new JLabel("LOCAL | Создатель");
    private final JProgressBar trustProgressBar = new JProgressBar(0, 100);
    private final JButton btnTrustPlus = new JButton("+10");
    private final JButton btnTrustMinus = new JButton("-10");
    private final JLabel creatorImmunityBadge = new JLabel("• Иммунитет Создателя");

    private final JProgressBar micVolumeBar = new JProgressBar(0, 800);
    private final JLabel dialogWindowLabel = new JLabel("Окно диалога: Ожидание");
    private final JComboBox<PersonalityPhase> phaseComboBox = new JComboBox<>(PersonalityPhase.values());

    // Кнопки управления каналами
    private final JButton btnToggleMic = new JButton("● Микрофон: ВКЛ");
    private final JButton btnToggleDiscord = new JButton("● Discord: ВКЛ");
    private final JButton btnToggleTwitch = new JButton("● Twitch: ВКЛ");

    private final JTextArea memoryInfoArea = new JTextArea();

    public SidebarPanel(DashboardStateService stateService, EmotionService emotionService,
                        RelationshipService relationshipService, MemoryService memoryService) {
        this.stateService = stateService;
        this.emotionService = emotionService;
        this.relationshipService = relationshipService;
        this.memoryService = memoryService;
        initEmotionLabels();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_APP);
        setPreferredSize(new Dimension(310, 0));

        add(buildProfileCard());
        add(Box.createVerticalStrut(8));
        add(buildMemoryCard());
        add(Box.createVerticalStrut(8));
        add(buildChannelsControlCard()); // Новая карточка управления каналами!
        add(Box.createVerticalStrut(8));
        add(buildPhaseCard());
        add(Box.createVerticalStrut(8));
        add(buildMoodCard());
        add(Box.createVerticalGlue());
    }

    private void initEmotionLabels() {
        emotionLabels.put(EmotionType.JOY, "Радость");
        emotionLabels.put(EmotionType.AFFECTION, "Милота");
        emotionLabels.put(EmotionType.TRUST, "Доверие");
        emotionLabels.put(EmotionType.INTEREST, "Интерес");
        emotionLabels.put(EmotionType.SURPRISE, "Шок");
        emotionLabels.put(EmotionType.IRRITATION, "Злость");
        emotionLabels.put(EmotionType.SADNESS, "Грусть");
        emotionLabels.put(EmotionType.FATIGUE, "Усталость");
    }

    private JPanel buildProfileCard() {
        JPanel card = createCard("АКТИВНЫЙ СОБЕСЕДНИК");

        speakerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        speakerLabel.setForeground(TEXT_PRIMARY);

        platformBadge.setFont(FONT_MONO);
        platformBadge.setForeground(TEXT_MUTED);

        trustProgressBar.setValue(100);
        trustProgressBar.setStringPainted(true);
        trustProgressBar.setForeground(ACCENT_GREEN);
        trustProgressBar.setBackground(BG_ELEVATED);
        trustProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        creatorImmunityBadge.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        creatorImmunityBadge.setForeground(new Color(245, 158, 11));

        JPanel trustBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        trustBtnRow.setBackground(BG_CARD);
        btnTrustPlus.addActionListener(e -> adjustCurrentTrust(10));
        btnTrustMinus.addActionListener(e -> adjustCurrentTrust(-10));
        trustBtnRow.add(new JLabel("Доверие:"));
        trustBtnRow.add(btnTrustPlus);
        trustBtnRow.add(btnTrustMinus);
        trustBtnRow.add(creatorImmunityBadge);

        card.add(speakerLabel);
        card.add(platformBadge);
        card.add(Box.createVerticalStrut(8));
        card.add(trustProgressBar);
        card.add(Box.createVerticalStrut(4));
        card.add(trustBtnRow);
        return card;
    }

    private void adjustCurrentTrust(int delta) {
        UserEntity currentUser = stateService.getCurrentActiveUser();
        if (currentUser != null && !currentUser.isCreator()) {
            relationshipService.adjustTrust(currentUser, delta);
            stateService.setTrustLevel(currentUser.getTrustLevel());
        }
    }

    private JPanel buildMemoryCard() {
        JPanel card = createCard("ПАМЯТЬ О СОБЕСЕДНИКЕ");

        memoryInfoArea.setEditable(false);
        memoryInfoArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        memoryInfoArea.setBackground(BG_ELEVATED);
        memoryInfoArea.setForeground(TEXT_PRIMARY);
        memoryInfoArea.setLineWrap(true);
        memoryInfoArea.setWrapStyleWord(true);
        memoryInfoArea.setMargin(new Insets(6, 6, 6, 6));
        memoryInfoArea.setText("Память пока чиста.");

        JScrollPane scroll = new JScrollPane(memoryInfoArea);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(280, 55));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        card.add(scroll);
        return card;
    }

    private JPanel buildChannelsControlCard() {
        JPanel card = createCard("АКТИВНОСТЬ КАНАЛОВ (STREAM MODE)");

        micVolumeBar.setForeground(ACCENT_BLURPLE);
        micVolumeBar.setBackground(BG_ELEVATED);
        micVolumeBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));

        btnToggleMic.addActionListener(e -> {
            boolean isMuted = !stateService.isMicMuted();
            stateService.setMicMuted(isMuted);
            updateButtonState(btnToggleMic, "Микрофон", isMuted);
            System.out.println("[Microphone Toggle] Микрофон переключен: " + (isMuted ? "ВЫКЛЮЧЕН (Muted)" : "ВКЛЮЧЕН (Active)"));
        });

        btnToggleDiscord.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnToggleDiscord.setForeground(ACCENT_GREEN);
        btnToggleDiscord.addActionListener(e -> {
            boolean isMuted = !stateService.isDiscordMuted();
            stateService.setDiscordMuted(isMuted);
            updateButtonState(btnToggleDiscord, "Discord", isMuted);
        });

        btnToggleTwitch.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnToggleTwitch.setForeground(ACCENT_GREEN);
        btnToggleTwitch.addActionListener(e -> {
            boolean isMuted = !stateService.isTwitchMuted();
            stateService.setTwitchMuted(isMuted);
            updateButtonState(btnToggleTwitch, "Twitch", isMuted);
        });

        dialogWindowLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        dialogWindowLabel.setForeground(TEXT_MUTED);

        card.add(micVolumeBar);
        card.add(Box.createVerticalStrut(6));
        card.add(btnToggleMic);
        card.add(Box.createVerticalStrut(4));
        card.add(btnToggleDiscord);
        card.add(Box.createVerticalStrut(4));
        card.add(btnToggleTwitch);
        card.add(Box.createVerticalStrut(6));
        card.add(dialogWindowLabel);
        return card;
    }

    private void updateButtonState(JButton btn, String name, boolean isMuted) {
        btn.setText(isMuted ? "● " + name + ": ВЫКЛ" : "● " + name + ": ВКЛ");
        btn.setForeground(isMuted ? ACCENT_RED : ACCENT_GREEN);
    }

    private JPanel buildPhaseCard() {
        JPanel card = createCard("ФАЗА ХАРАКТЕРА");

        phaseComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        phaseComboBox.setSelectedItem(stateService.getCurrentPhase());
        phaseComboBox.addActionListener(e -> {
            PersonalityPhase selected = (PersonalityPhase) phaseComboBox.getSelectedItem();
            if (selected != null) stateService.setCurrentPhase(selected);
        });

        card.add(phaseComboBox);
        return card;
    }

    private JPanel buildMoodCard() {
        JPanel card = createCard("РУЧНОЕ НАСТРОЕНИЕ");

        JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
        grid.setBackground(BG_CARD);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        for (EmotionType emotion : EmotionType.values()) {
            JButton btn = new JButton(emotionLabels.get(emotion));
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.addActionListener(e -> {
                stateService.setForceEmotionOverride(true);
                stateService.setCurrentEmotion(emotion);
                emotionService.setEmotion(emotion);
            });
            moodButtons.put(emotion, btn);
            grid.add(btn);
        }

        JButton btnAuto = new JButton("Авто");
        btnAuto.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAuto.addActionListener(e -> stateService.setForceEmotionOverride(false));
        grid.add(btnAuto);

        card.add(grid);
        return card;
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        titleLabel.setForeground(TEXT_MUTED);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        return panel;
    }

    public void refresh() {
        speakerLabel.setText(stateService.getCurrentSpeaker());
        platformBadge.setText(stateService.getCurrentPlatform() + " | " + stateService.getRelationship());
        trustProgressBar.setValue(stateService.getTrustLevel());
        trustProgressBar.setString(stateService.getTrustLevel() + "%");

        UserEntity current = stateService.getCurrentActiveUser();
        boolean isCreator = current != null ? current.isCreator() : "Мабучи".equalsIgnoreCase(stateService.getCurrentSpeaker());
        btnTrustPlus.setVisible(!isCreator);
        btnTrustMinus.setVisible(!isCreator);
        creatorImmunityBadge.setVisible(isCreator);

        int trust = stateService.getTrustLevel();
        trustProgressBar.setValue(trust);
        trustProgressBar.setString(trust + "%");

        if (trust >= 90) {
            trustProgressBar.setForeground(new Color(34, 197, 94)); // Зеленый (Любимчик)
        } else if (trust >= 70) {
            trustProgressBar.setForeground(new Color(16, 185, 129)); // Изумрудный (Друг)
        } else if (trust >= 40) {
            trustProgressBar.setForeground(new Color(99, 102, 241)); // Индиго (Нейтрал)
        } else if (trust >= 15) {
            trustProgressBar.setForeground(new Color(245, 158, 11)); // Оранжевый (Подозрительный)
        } else {
            trustProgressBar.setForeground(new Color(239, 68, 68));  // Красный (Враг)
        }

        int vol = (int) stateService.getCurrentMicVolume();
        micVolumeBar.setValue(Math.min(vol, 800));

        if (stateService.isDialogWindowActive()) {
            dialogWindowLabel.setText("● Диалог активен (" + stateService.getDialogWindowRemainingSeconds() + "с)");
            dialogWindowLabel.setForeground(ACCENT_GREEN);
        } else {
            dialogWindowLabel.setText("○ Окно закрыто (назови имя)");
            dialogWindowLabel.setForeground(TEXT_MUTED);
        }

        if (current != null) {
            List<String> facts = memoryService.getUserFacts(current);
            List<String> blunders = memoryService.getUserBlunders(current);

            if (facts.isEmpty() && blunders.isEmpty()) {
                memoryInfoArea.setText("Память пока чиста.");
            } else {
                StringBuilder sb = new StringBuilder();
                if (!facts.isEmpty()) {
                    sb.append("● Факты:\n");
                    for (String f : facts) sb.append("  • ").append(f).append("\n");
                }
                if (!blunders.isEmpty()) {
                    sb.append("● Косяки:\n");
                    for (String b : blunders) sb.append("  • ").append(b).append("\n");
                }
                memoryInfoArea.setText(sb.toString().trim());
            }
        }
    }
}