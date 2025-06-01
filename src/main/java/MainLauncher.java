import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.Animator;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainLauncher extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);
    private final AcrylicPanel acrylicPanel;
    private JTextArea resultArea;
    private JTextArea executionLogArea;
    private final AtomicBoolean isExecuting = new AtomicBoolean(false);
    private int executionDelay = 500;
    private String playerName = "@s";
    private List<String> generatedCommands = new ArrayList<>();
    private JTextField playerField;
    private JTextField delayField;
    private Thread executionThread;
    private JLabel cpuLabel;
    private JLabel memLabel;
    private JLabel timeLabel;
    private JLabel networkLabel;
    private int totalCommands;

    private static final Map<String, String> BLOCK_MAPPING = new HashMap<>();
    static {
        BLOCK_MAPPING.put("草方块", "grass_block");
        BLOCK_MAPPING.put("石头", "stone");
        BLOCK_MAPPING.put("橡木木板", "oak_planks");
        BLOCK_MAPPING.put("玻璃", "glass");
        BLOCK_MAPPING.put("铁块", "iron_block");
        BLOCK_MAPPING.put("金块", "gold_block");
        BLOCK_MAPPING.put("钻石块", "diamond_block");
        BLOCK_MAPPING.put("水", "water");
        BLOCK_MAPPING.put("熔岩", "lava");
        BLOCK_MAPPING.put("空气", "air");
    }

    public MainLauncher() {
        setupWindow();
        setupUI();
        acrylicPanel = new AcrylicPanel();
        setupDragListener();
    }

    private void setupWindow() {
        setUndecorated(true);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
        FlatLightLaf.setup();
        setBackground(new Color(255, 255, 255, 150));
    }

    private void setupUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setOpaque(false);
        rootPanel.add(createHeader(), BorderLayout.NORTH);
        rootPanel.add(mainPanel, BorderLayout.CENTER);
        rootPanel.add(createBottomMenu(), BorderLayout.SOUTH);
        mainPanel.add(createHomePanel(), "home");
        mainPanel.add(createAccountPanel(), "account");
        mainPanel.add(createSettingsPanel(), "settings");
        add(rootPanel);
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel paramPanel = new JPanel(new GridBagLayout());
        paramPanel.setOpaque(false);
        paramPanel.setBorder(BorderFactory.createTitledBorder("执行参数"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        paramPanel.add(new JLabel("玩家选择器:"), gbc);

        playerField = new JTextField(20);
        playerField.setText("Name");
        gbc.gridx = 1; gbc.gridy = 0;
        paramPanel.add(playerField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramPanel.add(new JLabel("执行间隔(ms):"), gbc);

        delayField = new JTextField(20);
        delayField.setText("500");
        gbc.gridx = 1; gbc.gridy = 1;
        paramPanel.add(delayField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        JButton startBtn = new JButton("开始执行");
        styleButton(startBtn);
        startBtn.addActionListener(e -> startExecution());

        JButton stopBtn = new JButton("停止执行");
        styleButton(stopBtn);
        stopBtn.addActionListener(e -> stopExecution());

        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.setBorder(BorderFactory.createTitledBorder("执行日志"));

        executionLogArea = new JTextArea(10, 40);
        executionLogArea.setEditable(false);
        executionLogArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12)); // 使用支持中文的字体
        executionLogArea.setText("// 执行日志将显示在这里\n");
        executionLogArea.append("// 请在点击'开始执行'后5秒内切换到Minecraft窗口!\n");
        executionLogArea.append("// 确保游戏聊天框可以正常使用（按T键打开）\n");

        JScrollPane scrollPane = new JScrollPane(executionLogArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel statusPanel = createStatusPanel();

        JPanel logAndStatusPanel = new JPanel(new BorderLayout());
        logAndStatusPanel.setOpaque(false);
        logAndStatusPanel.add(scrollPane, BorderLayout.CENTER);
        logAndStatusPanel.add(statusPanel, BorderLayout.SOUTH);

        logPanel.add(logAndStatusPanel, BorderLayout.CENTER);

        panel.add(paramPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(logPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("软件信息与声明");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("作者:"), gbc);

        JLabel authorLabel = new JLabel("CodeCubist");
        authorLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        gbc.gridx = 1;
        panel.add(authorLabel, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("B站主页:"), gbc);

        JLabel bilibiliLink = new JLabel("<html><a href='https://space.bilibili.com/3461580752685539'>点击访问作者B站主页</a></html>");
        bilibiliLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bilibiliLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://space.bilibili.com/3461580752685539"));
                } catch (Exception ignored) {
                }
            }
        });
        gbc.gridx = 1;
        panel.add(bilibiliLink, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("著作权:"), gbc);

        JLabel copyrightLabel = new JLabel("© 2025 CodeCubist. 保留所有权利");
        gbc.gridx = 1;
        panel.add(copyrightLabel, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        panel.add(new JLabel("开源协议:"), gbc);

        JLabel mitLicenseLink = new JLabel("<html><a href='https://opensource.org/licenses/MIT'>MIT License</a></html>");
        mitLicenseLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mitLicenseLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://opensource.org/licenses/MIT"));
                } catch (Exception ignored) {
                }
            }
        });
        gbc.gridx = 1;
        panel.add(mitLicenseLink, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("软件声明:"), gbc);

        JTextArea declarationArea = new JTextArea(8, 40);
        declarationArea.setText("""
                1. 本软件采用MIT开源协议分发
                2. 著作权归CodeCubist所有
                3. 本软件为免费软件，仅供学习交流使用
                4. 禁止用于任何商业用途或非法用途
                5. 作者不对使用本软件造成的任何损失负责
                6. 未经许可不得对本软件进行逆向工程或修改
                """);
        declarationArea.setEditable(false);
        declarationArea.setOpaque(false);
        declarationArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        declarationArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        gbc.gridy = 6;
        panel.add(declarationArea, gbc);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        cpuLabel = new JLabel("CPU: --%");
        cpuLabel.setForeground(Color.BLUE);
        panel.add(createStatusBox(cpuLabel, "CPU占用率"));

        memLabel = new JLabel("内存: --%");
        memLabel.setForeground(new Color(0, 128, 0)); // 深绿色
        panel.add(createStatusBox(memLabel, "内存占用率"));

        timeLabel = new JLabel("剩余: -- | 耗时: --");
        timeLabel.setForeground(Color.RED);
        panel.add(createStatusBox(timeLabel, "执行时间信息"));

        networkLabel = new JLabel("网络: --");
        networkLabel.setForeground(new Color(128, 0, 128)); // 紫色
        panel.add(createStatusBox(networkLabel, "网络类型"));
        initStatusTimer();
        return panel;
    }

    private JPanel createStatusBox(JComponent comp, String tooltip) {
        JPanel box = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createTitledBorder(tooltip));
        box.add(comp);
        return box;
    }

    private void initStatusTimer() {
        Timer statusTimer = new Timer(1000, e -> updateStatus());
        statusTimer.start();
    }

    private void updateStatus() {
        updateCpuUsage();
        updateMemoryUsage();
        updateNetworkType();
        updateTimeInfo();
    }

    private void updateCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getCpuLoad() * 100;
            cpuLabel.setText(String.format("CPU: %.1f%%", cpuLoad));
            if (cpuLoad > 80) {
                cpuLabel.setForeground(Color.RED);
            } else if (cpuLoad > 60) {
                cpuLabel.setForeground(Color.ORANGE);
            } else {
                cpuLabel.setForeground(Color.BLUE);
            }
        } catch (Exception e) {
            cpuLabel.setText("CPU: N/A");
        }
    }

    private void updateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMem = runtime.totalMemory();
        long freeMem = runtime.freeMemory();
        long usedMem = totalMem - freeMem;
        double memUsage = (usedMem * 100.0) / runtime.maxMemory();

        memLabel.setText(String.format("内存: %.1f%%", memUsage));

        if (memUsage > 90) {
            memLabel.setForeground(Color.RED);
        } else if (memUsage > 75) {
            memLabel.setForeground(Color.ORANGE);
        } else {
            memLabel.setForeground(new Color(0, 128, 0)); // 深绿色
        }
    }

    private void updateNetworkType() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String networkType = "未知";

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    String name = ni.getName().toLowerCase();
                    if (name.contains("wlan") || name.contains("wifi")) {
                        networkType = "WiFi";
                        break;
                    } else if (name.contains("eth") || name.contains("enet") || name.contains("lan")) {
                        networkType = "有线";
                        break;
                    } else if (name.contains("ppp") || name.contains("mobile")) {
                        networkType = "蜂窝网络";
                        break;
                    }
                }
            }

            networkLabel.setText("网络: " + networkType);
        } catch (Exception e) {
            networkLabel.setText("网络: 错误");
        }
    }

    private void updateTimeInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());

        if (isExecuting.get() && totalCommands > 0) {

            long totalTime = (long) totalCommands * executionDelay / 1000;

            timeLabel.setText(String.format(
                    "%s | 总估: %ds",
                    currentTime, totalTime
            ));
        } else {
            timeLabel.setText(currentTime + " | 未执行");
        }
        if (isExecuting.get()) {
            timeLabel.setForeground(Color.RED);
        } else {
            timeLabel.setForeground(Color.BLACK);
        }
    }

    private JPanel createBottomMenu() {
        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));

        createMenuButton("主页", "home", menuPanel);
        createMenuButton("执行指令", "account", menuPanel);
        createMenuButton("设置", "settings", menuPanel);

        return menuPanel;
    }


    private void createMenuButton(String text, String cardName, JPanel parent) {
        JButton btn = new HoverButton(text);
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        btn.addActionListener(e -> cardLayout.show(mainPanel, cardName));
        parent.add(btn);
    }

    private List<String> generateFillCommands(int x1, int y1, int z1,
                                              int x2, int y2, int z2,
                                              String block, boolean isClear) {
        List<String> commands = new ArrayList<>();
        final int MAX_BLOCKS = 32768;

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        int totalBlocks = width * height * depth;

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;

        commands.add(String.format("/tp %s %d %d %d", playerName, centerX, centerY, centerZ));

        if (totalBlocks <= MAX_BLOCKS) {
            commands.add(String.format("/fill %d %d %d %d %d %d %s",
                    minX, minY, minZ, maxX, maxY, maxZ, block));
        } else {
            int blocksPerSlice = MAX_BLOCKS / (width * height);
            int slices = (int) Math.ceil((double) depth / blocksPerSlice);

            for (int i = 0; i < slices; i++) {
                int startZ = minZ + i * blocksPerSlice;
                int endZ = Math.min(startZ + blocksPerSlice - 1, maxZ);

                commands.add(String.format("/fill %d %d %d %d %d %d %s",
                        minX, minY, startZ, maxX, maxY, endZ, block));
            }
        }

        commands.addFirst("// 命令总数: " + (commands.size() - 1));
        if (isClear) {
            commands.addFirst("// 清除方块命令（填充为空气）");
        } else {
            commands.addFirst("// 填充方块命令");
        }
        commands.addFirst("// 区域中心点: " + centerX + " " + centerY + " " + centerZ);
        return commands;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(218, 218, 218, 255));
        header.setPreferredSize(new Dimension(getWidth(), 40));

        JLabel title = new JLabel("Minecraft填充工具测试版");
        title.setForeground(new Color(50, 50, 50));
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);
        createControlButton("－", controls, e -> animateMinimize());
        createControlButton("×", controls, e -> animateClose());
        header.add(title, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private void animateMinimize() {
        Animator animator = new Animator(300, new Animator.TimingTarget() {
            @Override
            public void timingEvent(float fraction) {
                setOpacity(1 - fraction * 0.8f);
                setSize(
                        (int)(getWidth() * (1 - fraction * 0.2)),
                        (int)(getHeight() * (1 - fraction * 0.2))
                );
            }
            @Override
            public void end() {
                setState(Frame.ICONIFIED);
                setOpacity(1);
                setSize(1000, 600);
            }
        });
        animator.start();
    }

    private void animateClose() {
        Animator animator = new Animator(300, new Animator.TimingTarget() {
            @Override
            public void timingEvent(float fraction) {
                setOpacity(1 - fraction);
            }

            @Override
            public void end() {
                System.exit(0);
            }
        });
        animator.start();
    }

    private void createControlButton(String text, JPanel parent, ActionListener action) {
        JButton btn = new HoverButton(text);
        btn.addActionListener(action);
        parent.add(btn);
    }

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel horizontalPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        horizontalPanel.setOpaque(false);

        JPanel inputPanel = createInputPanel();
        horizontalPanel.add(inputPanel);

        JPanel resultPanel = createResultPanel();
        horizontalPanel.add(resultPanel);

        panel.add(horizontalPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("方块填充参数"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("起始坐标 (x y z):"), gbc);
        gbc.gridy = 1;
        panel.add(new JLabel("结束坐标 (x y z):"), gbc);
        gbc.gridy = 2;
        panel.add(new JLabel("方块类型:"), gbc);
        gbc.gridy = 3;
        panel.add(new JLabel("操作类型:"), gbc);

        JTextField startField = new JTextField(20);
        startField.setText("0 64 0");
        JTextField endField = new JTextField(20);
        endField.setText("10 70 10");

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(startField, gbc);
        gbc.gridy = 1;
        panel.add(endField, gbc);

        String[] blocks = {"草方块", "石头", "橡木木板", "玻璃", "铁块", "金块", "钻石块", "水", "熔岩", "空气"};
        JComboBox<String> blockCombo = new JComboBox<>(blocks);
        blockCombo.setSelectedItem("草方块");

        JTextField manualBlockField = new JTextField(20);
        manualBlockField.setEnabled(false);
        manualBlockField.setToolTipText("输入方块ID（例如：minecraft:stone）");

        JCheckBox manualInputCheck = new JCheckBox("手动输入方块ID");
        manualInputCheck.addItemListener(e -> {
            boolean manual = e.getStateChange() == ItemEvent.SELECTED;
            blockCombo.setEnabled(!manual);
            manualBlockField.setEnabled(manual);
        });

        JPanel blockInputPanel = new JPanel(new BorderLayout(5, 0));
        blockInputPanel.setOpaque(false);
        blockInputPanel.add(blockCombo, BorderLayout.CENTER);
        blockInputPanel.add(manualBlockField, BorderLayout.SOUTH);

        gbc.gridy = 2;
        panel.add(blockInputPanel, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        panel.add(manualInputCheck, gbc);

        ButtonGroup group = new ButtonGroup();
        JRadioButton fillBtn = new JRadioButton("填充方块", true);
        JRadioButton clearBtn = new JRadioButton("清除方块");
        JRadioButton replaceBtn = new JRadioButton("替换方块");
        group.add(fillBtn);
        group.add(clearBtn);
        group.add(replaceBtn);

        JTextField replaceTargetField = new JTextField(20);
        replaceTargetField.setEnabled(false);
        replaceTargetField.setToolTipText("输入要替换的方块ID（例如：minecraft:dirt）");

        ItemListener replaceFieldListener = e ->
                replaceTargetField.setEnabled(replaceBtn.isSelected());

        replaceBtn.addItemListener(replaceFieldListener);
        fillBtn.addItemListener(replaceFieldListener);
        clearBtn.addItemListener(replaceFieldListener);

        JPanel radioPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        radioPanel.setOpaque(false);
        radioPanel.add(fillBtn);
        radioPanel.add(clearBtn);
        radioPanel.add(replaceBtn);

        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(radioPanel, gbc);

        gbc.gridy = 4;
        panel.add(new JLabel("替换目标方块:"), gbc);
        gbc.gridy = 5;
        panel.add(replaceTargetField, gbc);

        JButton generateBtn = new JButton("生成命令");
        styleButton(generateBtn);
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(generateBtn, gbc);

        generateBtn.addActionListener(e -> {
            try {
                String startText = startField.getText().trim();
                String endText = endField.getText().trim();
                boolean isClear = clearBtn.isSelected();
                boolean isReplace = replaceBtn.isSelected();

                String[] startCoords = startText.split("\\s+");
                String[] endCoords = endText.split("\\s+");

                if (startCoords.length != 3 || endCoords.length != 3) {
                    throw new IllegalArgumentException("坐标格式不正确！请输入三个数字，用空格分隔");
                }

                int x1 = Integer.parseInt(startCoords[0]);
                int y1 = Integer.parseInt(startCoords[1]);
                int z1 = Integer.parseInt(startCoords[2]);

                int x2 = Integer.parseInt(endCoords[0]);
                int y2 = Integer.parseInt(endCoords[1]);
                int z2 = Integer.parseInt(endCoords[2]);

                String blockId;
                if (manualInputCheck.isSelected()) {
                    blockId = manualBlockField.getText().trim();
                    if (blockId.isEmpty()) {
                        throw new IllegalArgumentException("请输入方块ID");
                    }
                } else {
                    String block = (String) blockCombo.getSelectedItem();
                    if (block != null) {
                        blockId = BLOCK_MAPPING.getOrDefault(block, block.toLowerCase().replace(" ", "_"));
                    } else {
                        throw new IllegalArgumentException("请选择方块类型");
                    }
                }

                playerName = playerField.getText().trim(); // 获取玩家名称
                if (playerName.isEmpty()) playerName = "@a";

                if (isReplace) {
                    String replaceTarget = replaceTargetField.getText().trim();
                    if (replaceTarget.isEmpty()) {
                        throw new IllegalArgumentException("请输入要替换的方块ID");
                    }

                    generatedCommands = generateReplaceCommands(
                            x1, y1, z1,
                            x2, y2, z2,
                            blockId,
                            replaceTarget
                    );
                }
                else if (isClear) {
                    generatedCommands = generateFillCommands(
                            x1, y1, z1,
                            x2, y2, z2,
                            "air",
                            true
                    );
                }
                else {
                    generatedCommands = generateFillCommands(
                            x1, y1, z1,
                            x2, y2, z2,
                            blockId,
                            false
                    );
                }

                StringBuilder sb = new StringBuilder();
                for (String cmd : generatedCommands) {
                    sb.append(cmd).append("\n");
                }
                resultArea.setText(sb.toString());

            } catch (Exception ex) {
                resultArea.setText("错误: " + ex.getMessage());
            }
        });

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("生成的命令"));

        resultArea = new JTextArea(8, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12)); // 修复中文显示问题
        resultArea.setText("// 在此处显示生成的Minecraft命令");

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JButton copyBtn = new JButton("复制命令");
        styleButton(copyBtn);
        copyBtn.addActionListener(e -> {
            if (!resultArea.getText().isEmpty()) {
                resultArea.selectAll();
                resultArea.copy();
                JOptionPane.showMessageDialog(this, "命令已复制到剪贴板！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(copyBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void styleButton(JButton button) {
        button.setPreferredSize(new Dimension(150, 40));
        button.setForeground(new Color(78, 78, 78));
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240, 180));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                        BorderFactory.createEmptyBorder(5, 20, 5, 20)
                ));
    }

    private void setupDragListener() {
        MouseAdapter ma = new MouseAdapter() {
            private Point dragStart;

            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }

            public void mouseDragged(MouseEvent e) {
                Point current = e.getLocationOnScreen();
                setLocation(
                        current.x - dragStart.x,
                        current.y - dragStart.y);
            }
        };
        acrylicPanel.addMouseListener(ma);
        acrylicPanel.addMouseMotionListener(ma);

        DragAdapter dragAdapter = new DragAdapter();

        acrylicPanel.addMouseListener(dragAdapter);
        acrylicPanel.addMouseMotionListener(dragAdapter);
        getContentPane().addMouseListener(dragAdapter);
        getContentPane().addMouseMotionListener(dragAdapter);
    }

    private class DragAdapter extends MouseAdapter {
        private Point dragStart;
        private Point frameStart;
        private boolean dragging = false;
        private static final int DRAG_THRESHOLD = 5;

        @Override
        public void mousePressed(MouseEvent e) {
            dragStart = e.getLocationOnScreen();
            frameStart = getLocation();
            dragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                Point current = e.getLocationOnScreen();
                if (Math.abs(current.x - dragStart.x) < DRAG_THRESHOLD &&
                        Math.abs(current.y - dragStart.y) < DRAG_THRESHOLD) {
                    return;
                }
                dragging = true;
            }

            Point current = e.getLocationOnScreen();
            int newX = frameStart.x + (current.x - dragStart.x);
            int newY = frameStart.y + (current.y - dragStart.y);

            Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds();
            newX = Math.max(screenBounds.x, Math.min(newX,
                    screenBounds.x + screenBounds.width - getWidth()));
            newY = Math.max(screenBounds.y, Math.min(newY,
                    screenBounds.y + screenBounds.height - getHeight()));

            setLocation(newX, newY);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
        }
    }

    static class HoverButton extends JButton {
        private Animator animator;

        public HoverButton(String text) {
            super(text);
            putClientProperty("JButton.buttonType", "none");
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(new Color(50, 50, 50));
            setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    startAnimator();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    startAnimator();
                }
            });
        }

        private void startAnimator() {
            if (animator != null && animator.isRunning()) {
                animator.stop();
            }

            animator = new Animator(200, new Animator.TimingTarget() {
                @Override
                public void timingEvent(float fraction) {
                    repaint();
                }

                @Override
                public void end() {
                }
            });
            animator.setResolution(5);
            animator.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(0, 0, 0, 0));
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (getModel().isRollover()) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }

            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            Rectangle textBounds = fm.getStringBounds(this.getText(), g2).getBounds();
            int textX = (getWidth() - textBounds.width) / 2;
            int textY = (getHeight() - textBounds.height) / 2 + fm.getAscent();
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    private void startExecution() {
        if (generatedCommands.isEmpty()) {
            executionLogArea.append("错误: 没有可执行的命令! 请先生成命令\n");
            return;
        }

        try {
            executionDelay = Integer.parseInt(delayField.getText().trim());
            if (executionDelay < 0) {
                executionDelay = 0;
                delayField.setText("0");
            }
        } catch (NumberFormatException ex) {
            executionDelay = 500;
            delayField.setText("500");
        }

        playerName = playerField.getText().trim();
        if (playerName.isEmpty()) {
            playerName = "@a";
        }

        if (isExecuting.get()) {
            executionLogArea.append("执行正在进行中，请先停止当前执行\n");
            return;
        }
        totalCommands = 0;
        for (String command : generatedCommands) {
            if (!command.startsWith("//")) {
                totalCommands++;
            }
        }

        isExecuting.set(true);
        executionLogArea.append("开始执行命令...\n");
        executionLogArea.append("请在5秒内切换到Minecraft窗口!\n");
        executionLogArea.append("玩家选择器: " + playerName + "\n");
        executionLogArea.append("执行间隔: " + executionDelay + "ms\n");

        executionThread = new Thread(() -> {
            try {
                Robot robot = new Robot();

                for (int i = 5; i > 0; i--) {
                    if (!isExecuting.get()) break;
                    final int count = i;
                    SwingUtilities.invokeLater(() -> executionLogArea.append("倒计时: " + count + " 秒...\n"));
                    Thread.sleep(1000);
                }

                if (!isExecuting.get()) {
                    SwingUtilities.invokeLater(() -> executionLogArea.append("执行已取消\n"));
                    return;
                }

                SwingUtilities.invokeLater(() -> executionLogArea.append("开始执行命令序列...\n"));

                for (String command : generatedCommands) {
                    if (!isExecuting.get()) break;

                    if (command.startsWith("//")) {
                        SwingUtilities.invokeLater(() -> executionLogArea.append("跳过注释: " + command + "\n"));
                        continue;
                    }

                    SwingUtilities.invokeLater(() -> executionLogArea.append("执行: " + command + "\n"));

                    copyToClipboard(command);

                    robot.keyPress(KeyEvent.VK_T);
                    robot.keyRelease(KeyEvent.VK_T);
                    robot.delay(100); // 等待聊天框打开

                    robot.keyPress(KeyEvent.VK_CONTROL);
                    robot.keyPress(KeyEvent.VK_V);
                    robot.keyRelease(KeyEvent.VK_V);
                    robot.keyRelease(KeyEvent.VK_CONTROL);
                    robot.delay(100);

                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);

                    if (command.startsWith("/tp")) {
                        Thread.sleep(2000);
                    } else {
                        Thread.sleep(executionDelay);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    if (isExecuting.get()) {
                        executionLogArea.append("\n所有命令执行完成!\n");
                    } else {
                        executionLogArea.append("\n执行已停止\n");
                    }
                });

            } catch (AWTException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> executionLogArea.append("执行错误: " + ex.getMessage() + "\n"));
            } finally {
                isExecuting.set(false);
            }
        });

        executionThread.start();
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
    }

    private List<String> generateReplaceCommands(int x1, int y1, int z1,
                                                 int x2, int y2, int z2,
                                                 String newBlock, String oldBlock) {
        List<String> commands = new ArrayList<>();
        final int MAX_BLOCKS = 32768;

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        int totalBlocks = width * height * depth;

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;

        commands.add(String.format("/tp %s %d %d %d", playerName, centerX, centerY, centerZ));

        if (totalBlocks <= MAX_BLOCKS) {
            commands.add(String.format("/fill %d %d %d %d %d %d %s replace %s",
                    minX, minY, minZ, maxX, maxY, maxZ, newBlock, oldBlock));
        } else {
            int blocksPerSlice = MAX_BLOCKS / (width * height);
            int slices = (int) Math.ceil((double) depth / blocksPerSlice);

            for (int i = 0; i < slices; i++) {
                int startZ = minZ + i * blocksPerSlice;
                int endZ = Math.min(startZ + blocksPerSlice - 1, maxZ);

                commands.add(String.format("/fill %d %d %d %d %d %d %s replace %s",
                        minX, minY, startZ, maxX, maxY, endZ, newBlock, oldBlock));
            }
        }

        commands.addFirst("// 命令总数: " + (commands.size() - 1));
        commands.addFirst("// 替换命令 - 将 " + oldBlock + " 替换为 " + newBlock);
        commands.addFirst("// 区域中心点: " + centerX + " " + centerY + " " + centerZ);

        return commands;
    }

    private void stopExecution() {
        if (isExecuting.get()) {
            isExecuting.set(false);
            executionLogArea.append("正在停止执行...\n");

            if (executionThread != null && executionThread.isAlive()) {
                executionThread.interrupt();
            }
        } else {
            executionLogArea.append("当前没有正在执行的命令\n");
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            MainLauncher launcher = new MainLauncher();
            launcher.setVisible(true);
        });
    }
}