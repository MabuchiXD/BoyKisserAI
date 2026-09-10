package org.example.boykisserai.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import jakarta.annotation.PostConstruct;
import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.memory.MemoryService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.example.boykisserai.service.state.EmotionService;
import org.example.boykisserai.service.relationship.RelationshipService;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Component
public class DashboardGuiFrame extends JFrame {

    public static final Color BG_APP = new Color(18, 18, 20);
    public static final Color BG_CARD = new Color(26, 26, 30);
    public static final Color BG_ELEVATED = new Color(38, 38, 44);
    public static final Color BORDER_COLOR = new Color(42, 42, 48);
    public static final Color TEXT_PRIMARY = new Color(244, 244, 245);
    public static final Color TEXT_MUTED = new Color(130, 130, 140);
    public static final Color ACCENT_BLURPLE = new Color(99, 102, 241);
    public static final Color ACCENT_GREEN = new Color(34, 197, 94);
    public static final Color ACCENT_RED = new Color(239, 68, 68);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_MONO = new Font("Segoe UI Semibold", Font.PLAIN, 12);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 18);

    private final DashboardStateService stateService;
    private final EmotionService emotionService;
    private final RelationshipService relationshipService;
    private final CharacterService characterService;
    private final MemoryService memoryService; // Добавили сервис памяти

    private HeaderStatsPanel headerStatsPanel;
    private SidebarPanel sidebarPanel;
    private ChannelsPanel channelsPanel;

    public DashboardGuiFrame(DashboardStateService stateService,
                             EmotionService emotionService,
                             RelationshipService relationshipService,
                             CharacterService characterService,
                             MemoryService memoryService) {
        this.stateService = stateService;
        this.emotionService = emotionService;
        this.relationshipService = relationshipService;
        this.characterService = characterService;
        this.memoryService = memoryService;
    }

    @PostConstruct
    public void init() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarkLaf.setup();

                UIManager.put("Button.arc", 10);
                UIManager.put("Component.arc", 10);
                UIManager.put("ProgressBar.arc", 10);
                UIManager.put("TextComponent.arc", 10);

                UIManager.put("ScrollBar.width", 7);
                UIManager.put("ScrollBar.thumbArc", 10);
                UIManager.put("ScrollBar.track", BG_APP);
                UIManager.put("ScrollBar.thumb", BG_ELEVATED);
                UIManager.put("ScrollBar.hoverThumbColor", ACCENT_BLURPLE);
                UIManager.put("ScrollBar.showButtons", false);

                UIManager.put("TabbedPane.tabHeight", 36);
                UIManager.put("TabbedPane.showTabSeparators", false);
                UIManager.put("TabbedPane.selectedBackground", BG_ELEVATED);
                UIManager.put("TabbedPane.background", BG_CARD);
                UIManager.put("TabbedPane.contentAreaColor", BG_CARD);
                UIManager.put("TabbedPane.underlineColor", ACCENT_BLURPLE);
                UIManager.put("TabbedPane.foreground", TEXT_MUTED);
                UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
                UIManager.put("TabbedPane.borderContentColor", BORDER_COLOR);

                UIManager.put("defaultFont", FONT_BODY);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Передаем memoryService в SidebarPanel!
            this.headerStatsPanel = new HeaderStatsPanel(stateService);
            this.sidebarPanel = new SidebarPanel(stateService, emotionService, relationshipService, memoryService);
            this.channelsPanel = new ChannelsPanel(stateService, characterService);

            setupGui();
        });
    }

    private void setupGui() {
        setTitle("Boykisser AI — Панель Создателя");
        setSize(1150, 720);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(BG_APP);
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(mainPanel);

        mainPanel.add(headerStatsPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(12, 0));
        centerPanel.setBackground(BG_APP);
        centerPanel.add(sidebarPanel, BorderLayout.WEST);
        centerPanel.add(channelsPanel, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        Timer refreshTimer = new Timer(150, e -> {
            headerStatsPanel.refresh();
            sidebarPanel.refresh();
            channelsPanel.refresh();
        });
        refreshTimer.start();

        setVisible(true);
    }
}