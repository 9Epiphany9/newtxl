package com.txl.view;

import com.txl.dao.PersonalDao;
import com.txl.dean.personalInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class AddressBookApp extends JFrame {
    private String currentSelectedGroup = "未分组"; // 默认显示"未分组"
    private JPanel groupMembersPanel; // 组成员面板引用
    private static final String DATA_DIR = "data";
    private static final String CONTACTS_FILE = DATA_DIR + File.separator + "contacts.csv";

    private DefaultListModel<String> contactListModel;
    private JList<String> contactList;
    private JComboBox<String> filterComboBox;
    private DefaultListModel<String> groupListModel;
    private JList<String> groupList;
    private JTextField searchField;

    private DefaultListModel<String> groupMembersModel;
    private JList<String> groupMembersList;

    private Map<String, String> contactGroupMap = new HashMap<>();
    private PersonalDao personalDao;
    private JButton editButton;

    // 新增成员变量：存储用户选择的显示属性
    private String[] displayFields = {"姓名", "电话", "邮箱"};

    public AddressBookApp() {
        setTitle("通讯录管理系统");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        new File(DATA_DIR).mkdirs();
        personalDao = new PersonalDao(CONTACTS_FILE);

        initUI();
        loadContacts();
    }

    private void loadContacts() {
        contactListModel.clear();
        contactGroupMap.clear();
        try {
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                String name = contact.getName();
                contactListModel.addElement(name);
                contactGroupMap.put(name, String.join(", ", contact.getGroups()));
            }
            updateGroupMembersList();
            filterContacts();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "加载联系人失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        // 主面板 - 渐变背景
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(240, 245, 250);
                Color color2 = new Color(220, 230, 240);
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        // 左侧面板
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        // 联系人面板
        JPanel contactPanel = createStyledPanel("联系人", 320, 350);

        // 过滤面板
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("显示："));

        filterComboBox = new JComboBox<>(new String[]{"所有联系人", "未分组联系人"});
        styleComboBox(filterComboBox);
        filterComboBox.addActionListener(e -> filterContacts());
        filterPanel.add(filterComboBox);
        contactPanel.add(filterPanel, BorderLayout.NORTH);

        // 联系人列表
        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        styleList(contactList);
        contactList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    clearSelectionsExcept(contactList);
                    String selectedName = contactList.getSelectedValue();
                    if (selectedName != null) {
                        showContactInfo(selectedName);
                    }
                }
            }
        });
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedName = contactList.getSelectedValue();
                if (selectedName != null) {
                    clearSelectionsExcept(contactList);
                    enableEditButton(true);
                } else {
                    enableEditButton(false);
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(contactList);
        styleScrollPane(listScrollPane);
        contactPanel.add(listScrollPane, BorderLayout.CENTER);

        // 联系组面板
        JPanel groupPanel = createStyledPanel("联系组", 320, 180);

        // 初始化分组列表
        groupListModel = new DefaultListModel<>();
        loadGroupList();

        groupList = new JList<>(groupListModel);
        styleList(groupList);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentSelectedGroup = groupList.getSelectedValue();
                System.out.println("选中组: " + currentSelectedGroup);
                updateGroupMembersList();
            }
        });
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        styleScrollPane(groupScrollPane);
        groupPanel.add(groupScrollPane, BorderLayout.CENTER);

        // 组管理按钮
        JPanel groupButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        groupButtonPanel.setOpaque(false);

        JButton addGroupButton = createStyledButton("新增分组", new Color(60, 179, 113));
        JButton deleteGroupButton = createStyledButton("删除分组", new Color(205, 92, 92));

        addGroupButton.addActionListener(e -> showAddGroupDialog());
        deleteGroupButton.addActionListener(e -> showDeleteGroupDialog());

        groupButtonPanel.add(addGroupButton);
        groupButtonPanel.add(deleteGroupButton);
        groupPanel.add(groupButtonPanel, BorderLayout.SOUTH);

        // 添加联系人和联系组到左侧
        leftPanel.add(contactPanel);
        leftPanel.add(Box.createVerticalStrut(15));
        leftPanel.add(groupPanel);

        // 右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        // 创建顶部工具栏
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbarPanel.setOpaque(false);

        JButton importButton = createStyledButton("导入", new Color(100, 149, 237));
        JButton exportButton = createStyledButton("导出", new Color(100, 149, 237));

        importButton.addActionListener(e -> showImportDialog());
        exportButton.addActionListener(e -> showExportDialog());

        toolbarPanel.add(importButton);
        toolbarPanel.add(exportButton);

        // 添加搜索面板到工具栏
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        searchField = new JTextField(20);
        styleTextField(searchField);

        JButton searchButton = createStyledButton("搜索", new Color(70, 130, 180));
        searchButton.addActionListener(e -> searchContacts());
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // 添加显示设置按钮到工具栏
        JButton displaySettingsButton = createStyledButton("显示设置", new Color(100, 149, 237));
        displaySettingsButton.addActionListener(e -> showDisplaySettingsDialog());

        toolbarPanel.add(searchPanel);
        toolbarPanel.add(Box.createHorizontalStrut(10));
        toolbarPanel.add(displaySettingsButton);

        rightPanel.add(toolbarPanel, BorderLayout.NORTH);

        // 组成员面板保持不变
        groupMembersPanel = createStyledPanel("组成员 - " + currentSelectedGroup, 450, 400);
        groupMembersPanel.setLayout(new BorderLayout());
        groupMembersModel = new DefaultListModel<>();
        groupMembersList = new JList<>(groupMembersModel);
        styleList(groupMembersList);
        groupMembersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        groupMembersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedName = groupMembersList.getSelectedValue();
                    if (selectedName != null && !selectedName.startsWith("姓名")) {
                        showContactInfo(selectedName.split(" ")[0]);
                    }
                }
            }
        });

        groupMembersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                enableEditButton(!groupMembersList.isSelectionEmpty());
            }
        });

        JScrollPane groupMembersScrollPane = new JScrollPane(groupMembersList);
        styleScrollPane(groupMembersScrollPane);
        groupMembersPanel.add(groupMembersScrollPane, BorderLayout.CENTER);

        rightPanel.add(groupMembersPanel, BorderLayout.CENTER);

        // 底部按钮面板重新组织
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setOpaque(false);

        // 第一行按钮：联系人操作
        JPanel contactButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        contactButtonsPanel.setOpaque(false);

        JButton addButton = createStyledButton("添加联系人", new Color(60, 179, 113));
        JButton deleteButton = createStyledButton("删除联系人", new Color(205, 92, 92));
        editButton = createStyledButton("编辑联系人", new Color(70, 130, 180));

        setupButtonListeners(addButton, deleteButton, editButton, searchButton);

        contactButtonsPanel.add(addButton);
        contactButtonsPanel.add(deleteButton);
        contactButtonsPanel.add(editButton);

        // 第二行按钮：分组操作
        JPanel groupButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        groupButtonsPanel.setOpaque(false);

        JButton moveToGroupButton = createStyledButton("移动到分组", new Color(70, 130, 180));
        JButton removeFromGroupButton = createStyledButton("从分组移除", new Color(205, 92, 92));

        moveToGroupButton.addActionListener(e -> showMoveToGroupDialog());
        removeFromGroupButton.addActionListener(e -> removeFromGroup());

        groupButtonsPanel.add(moveToGroupButton);
        groupButtonsPanel.add(removeFromGroupButton);

        // 将两行按钮面板添加到主按钮面板
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(contactButtonsPanel);
        buttonPanel.add(groupButtonsPanel);

        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加到主面板
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
    }

    private void loadGroupList() {
        Set<String> groups = new HashSet<>();
        try {
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                groups.addAll(contact.getGroups()); // 替换原 contact.getGroup()
            }
        } catch (IOException e) {
            System.err.println("加载分组失败: " + e.getMessage());
        }

        // 按字母顺序排序
        List<String> sortedGroups = new ArrayList<>(groups);
        Collections.sort(sortedGroups);

        // 添加到分组列表
        groupListModel.clear();
        for (String group : sortedGroups) {
            groupListModel.addElement(group);
        }
    }

    private void showAddGroupDialog() {
        String groupName = JOptionPane.showInputDialog(
                this,
                "请输入新分组名称:",
                "新增分组",
                JOptionPane.PLAIN_MESSAGE
        );

        if (groupName != null && !groupName.trim().isEmpty()) {
            groupName = groupName.trim();

            // 验证分组名称
            if (groupName.contains(",")) {
                JOptionPane.showMessageDialog(
                        this,
                        "分组名称不能包含逗号!",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            if (groupName.length() > 20) {
                JOptionPane.showMessageDialog(
                        this,
                        "分组名称不能超过20个字符!",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // 检查分组是否已存在
            if (groupListModel.contains(groupName)) {
                JOptionPane.showMessageDialog(
                        this,
                        "分组 \"" + groupName + "\" 已存在!",
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // 添加到分组列表
            groupListModel.addElement(groupName);

            // 排序分组列表
            List<String> groups = new ArrayList<>();
            for (int i = 0; i < groupListModel.size(); i++) {
                groups.add(groupListModel.getElementAt(i));
            }
            Collections.sort(groups);

            groupListModel.clear();
            for (String group : groups) {
                groupListModel.addElement(group);
            }

            JOptionPane.showMessageDialog(
                    this,
                    "分组 \"" + groupName + "\" 添加成功!",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void showDeleteGroupDialog() {
        String selectedGroup = groupList.getSelectedValue();

        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "请先选择一个要删除的分组!",
                    "提示",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if ("未分组".equals(selectedGroup)) {
            JOptionPane.showMessageDialog(
                    this,
                    "不能删除默认分组 \"未分组\"!",
                    "错误",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要删除分组 \"" + selectedGroup + "\" 吗?\n(该分组的联系人将被移动到\"未分组\")",
            "确认删除",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // 1. 获取该分组的所有联系人
                List<personalInfo> contacts = personalDao.loadAll();
                List<personalInfo> toUpdate = new ArrayList<>();

                for (personalInfo contact : contacts) {
                    if (contact.getGroups().contains(selectedGroup)) {
                        // 2. 将这些联系人的分组设置为"未分组"，移除其他分组
                        contact.setGroups(new HashSet<>(Arrays.asList("未分组")));
                        toUpdate.add(contact);
                    }
                }

                // 3. 批量更新这些联系人
                for (personalInfo contact : toUpdate) {
                    personalDao.update(contact);
                }

                // 4. 从分组列表中删除该分组
                groupListModel.removeElement(selectedGroup);

                // 5. 刷新界面
                loadContacts();

                JOptionPane.showMessageDialog(
                    this,
                    "分组 \"" + selectedGroup + "\" 已删除!\n" +
                            toUpdate.size() + " 个联系人已被移动到\"未分组\"",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "删除分组失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // 创建统一样式的面板
    private JPanel createStyledPanel(String title, int width, int height) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(width, height));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 200)),
                title
        );
        border.setTitleFont(new Font("微软雅黑", Font.BOLD, 14));
        border.setTitleColor(new Color(70, 70, 70));
        panel.setBorder(border);

        return panel;
    }

    // 更新按钮样式方法，添加最小宽度设置
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setMinimumSize(new Dimension(100, 30)); // 设置最小宽度
        button.setPreferredSize(new Dimension(120, 30));

        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker()),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }
    // 样式化列表
    private void styleList(JList<?> list) {
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        list.setBackground(new Color(250, 250, 255));
        list.setSelectionBackground(new Color(70, 130, 180));
        list.setSelectionForeground(Color.WHITE);
        list.setFixedCellHeight(28);
        list.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    // 样式化滚动面板
    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220)));
        scrollPane.getViewport().setBackground(new Color(250, 250, 255));
        JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL);
        scrollBar.setUI(new CustomScrollBarUI());
        scrollPane.setVerticalScrollBar(scrollBar);
    }

    // 样式化文本框
    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    // 样式化组合框
    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        comboBox.setBackground(Color.WHITE);
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = super.createArrowButton();
                button.setBackground(new Color(70, 130, 180));
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return button;
            }
        });
    }

    private void setupButtonListeners(JButton addButton, JButton deleteButton, JButton editButton, JButton searchButton) {
        // 添加联系人
        addButton.addActionListener(e -> showAddContactDialog());

        // 删除联系人
        deleteButton.addActionListener(e -> {
            String currentName = getSelectedContactName();
            if (currentName != null) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "确定要删除联系人 " + currentName + " 吗？",
                        "确认删除",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        personalDao.delete(currentName);
                        loadContacts();
                        JOptionPane.showMessageDialog(this, "联系人删除成功！");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this,
                                "删除联系人失败: " + ex.getMessage(),
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个联系人！");
            }
        });

        // 编辑联系人
        editButton.addActionListener(e -> {
            String currentName = getSelectedContactName();
            if (currentName != null) {
                showEditContactDialog(currentName);
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个联系人！");
            }
        });

        // 搜索联系人
        searchButton.addActionListener(e -> searchContacts());
    }

    private void showAddContactDialog() {
        // 创建对话框面板
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 表单面板
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));

        // 创建输入字段
        JTextField nameField = new JTextField();
        JTextField telephoneField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField homepageField = new JTextField();
        JTextField birthdayField = new JTextField();
        JTextField photoField = new JTextField();
        JTextField companyField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField zipCodeField = new JTextField();

        // 使用动态分组列表
        JComboBox<String> groupField = new JComboBox<>();
        for (int i = 0; i < groupListModel.size(); i++) {
            groupField.addItem(groupListModel.getElementAt(i));
        }

        JTextField notesField = new JTextField();

        // 照片选择按钮
        JButton photoButton = new JButton("选择照片");
        photoButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                photoField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        // 添加字段到表单
        addFormField(formPanel, "姓名 *:", nameField);
        addFormField(formPanel, "电话:", telephoneField);
        addFormField(formPanel, "邮箱:", emailField);
        addFormField(formPanel, "主页:", homepageField);
        addFormField(formPanel, "生日:", birthdayField);
        addFormField(formPanel, "照片:", photoButton);
        addFormField(formPanel, "单位:", companyField);
        addFormField(formPanel, "地址:", addressField);
        addFormField(formPanel, "邮编:", zipCodeField);
        addFormField(formPanel, "分组:", groupField);
        addFormField(formPanel, "备注:", notesField);

        panel.add(formPanel, BorderLayout.CENTER);

        // 显示对话框
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "添加联系人",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        // 处理用户输入
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "姓名不能为空！");
                return;
            }

            personalInfo contact = new personalInfo(name);
            contact.setTelephone(telephoneField.getText().trim());
            contact.setEmail(emailField.getText().trim());
            contact.setHomepage(homepageField.getText().trim());
            contact.setBirthday(birthdayField.getText().trim());
            contact.setPhoto(photoField.getText().trim());
            contact.setCompany(companyField.getText().trim());
            contact.setAddress(addressField.getText().trim());
            contact.setZipCode(zipCodeField.getText().trim());
            contact.setGroups(new HashSet<>(Arrays.asList((String) groupField.getSelectedItem())));
            contact.setNotes(notesField.getText().trim());

            try {
                personalDao.add(contact);
                loadContacts();
                loadGroupList(); // 刷新分组列表
                JOptionPane.showMessageDialog(this, "联系人添加成功！");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "保存联系人失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditContactDialog(String currentName) {
        personalInfo originalContact = getContactInfo(currentName);
        if (originalContact == null) {
            JOptionPane.showMessageDialog(this, "找不到联系人信息！");
            return;
        }

        // 创建对话框面板
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 表单面板
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));

        // 创建输入字段
        JTextField nameField = new JTextField(originalContact.getName());
        JTextField telephoneField = new JTextField(originalContact.getTelephone());
        JTextField emailField = new JTextField(originalContact.getEmail());
        JTextField homepageField = new JTextField(originalContact.getHomepage());
        JTextField birthdayField = new JTextField(originalContact.getBirthday());
        JTextField photoField = new JTextField(originalContact.getPhoto());
        JTextField companyField = new JTextField(originalContact.getCompany());
        JTextField addressField = new JTextField(originalContact.getAddress());
        JTextField zipCodeField = new JTextField(originalContact.getZipCode());

        // 使用动态分组列表
        JList<String> groupField = new JList<>(groupListModel);
        groupField.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        groupField.setSelectedIndices(getSelectedIndicesForGroups(originalContact.getGroups()));

        JTextField notesField = new JTextField(originalContact.getNotes());

        // 照片选择按钮
        JButton photoButton = new JButton("选择照片");
        photoButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                photoField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        // 添加字段到表单
        addFormField(formPanel, "姓名 *:", nameField);
        addFormField(formPanel, "电话:", telephoneField);
        addFormField(formPanel, "邮箱:", emailField);
        addFormField(formPanel, "主页:", homepageField);
        addFormField(formPanel, "生日:", birthdayField);
        addFormField(formPanel, "照片:", photoButton);
        addFormField(formPanel, "单位:", companyField);
        addFormField(formPanel, "地址:", addressField);
        addFormField(formPanel, "邮编:", zipCodeField);
        addFormField(formPanel, "分组:", new JScrollPane(groupField)); // 使用JScrollPane包裹JList
        addFormField(formPanel, "备注:", notesField);

        panel.add(formPanel, BorderLayout.CENTER);

        // 创建对话框
        JDialog dialog = new JDialog(this, "编辑联系人", true);
        dialog.setContentPane(panel);
        dialog.setSize(400, 500); // 设置对话框大小
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 确认按钮
        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "姓名不能为空！");
                return;
            }

            personalInfo contact = new personalInfo(name);
            contact.setTelephone(telephoneField.getText().trim());
            contact.setEmail(emailField.getText().trim());
            contact.setHomepage(homepageField.getText().trim());
            contact.setBirthday(birthdayField.getText().trim());
            contact.setPhoto(photoField.getText().trim());
            contact.setCompany(companyField.getText().trim());
            contact.setAddress(addressField.getText().trim());
            contact.setZipCode(zipCodeField.getText().trim());

            // 处理分组更新 - 保留原有分组
            Set<String> newGroups = new HashSet<>(groupField.getSelectedValuesList());
            if (newGroups.isEmpty()) {
                newGroups.add("未分组"); // 确保至少有一个分组
            }
            contact.setGroups(newGroups);

            contact.setNotes(notesField.getText().trim());

            try {
                personalDao.update(contact);
                loadContacts();
                loadGroupList(); // 刷新分组列表
                JOptionPane.showMessageDialog(this, "联系人编辑成功！");
                dialog.dispose();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "保存联系人失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // 取消按钮
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dialog.dispose());

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 显示对话框
        dialog.setVisible(true);
    }

    // 新增方法：根据分组名称获取对应的索引
    private int[] getSelectedIndicesForGroups(Set<String> groups) {
        List<Integer> indices = new ArrayList<>();
        for (String group : groups) {
            for (int i = 0; i < groupListModel.size(); i++) {
                if (groupListModel.getElementAt(i).equals(group)) {
                    indices.add(i);
                    break;
                }
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private void addFormField(JPanel panel, String label, JComponent field) {
        JLabel jLabel = new JLabel(label);
        jLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        panel.add(jLabel);

        if (field instanceof JTextField) {
            styleTextField((JTextField) field);
        } else if (field instanceof JComboBox) {
            styleComboBox((JComboBox<String>) field);
        }

        panel.add(field);
    }

    private void searchContacts() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (!keyword.isEmpty()) {
            try {
                List<personalInfo> contacts = personalDao.loadAll();
                DefaultListModel<String> filteredModel = new DefaultListModel<>();

                for (personalInfo contact : contacts) {
                    if (contact.getName().toLowerCase().contains(keyword) ||
                            (contact.getTelephone() != null && contact.getTelephone().toLowerCase().contains(keyword)) ||
                            (contact.getEmail() != null && contact.getEmail().toLowerCase().contains(keyword))) {
                        filteredModel.addElement(contact.getName());
                    }
                }
                contactList.setModel(filteredModel);

                String message = String.format("找到 %d 个匹配的联系人", filteredModel.getSize());
                JOptionPane.showMessageDialog(this, message);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "搜索联系人失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                contactList.setModel(contactListModel);
            }
        } else {
            contactList.setModel(contactListModel);
        }
    }

    private String getSelectedContactName() {
        List<String> names = getSelectedContactNames();
        return names.isEmpty() ? null : names.get(0);
    }

    private void filterContacts() {
        String filter = (String) filterComboBox.getSelectedItem();
        DefaultListModel<String> filteredModel = new DefaultListModel<>();
        for (int i = 0; i < contactListModel.size(); i++) {
            String contact = contactListModel.get(i);
            String group = contactGroupMap.get(contact);
            if ("所有联系人".equals(filter) || ("未分组联系人".equals(filter) && "未分组".equals(group))) {
                filteredModel.addElement(contact);
            }
        }
        contactList.setModel(filteredModel);
    }

    private personalInfo getContactInfo(String name) {
        try {
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                if (contact.getName().equals(name)) {
                    return contact;
                }
            }
        } catch (IOException e) {
            System.err.println("获取联系人信息失败: " + e.getMessage());
        }
        return null;
    }

    private void updateGroupMembersList() {
        groupMembersModel.clear();

        // 默认显示字段（如果没有设置）
        String[] fieldsToDisplay = this.displayFields != null ? this.displayFields
                             : new String[]{"姓名", "电话", "邮箱"};

        try {
            // 生成表头
            StringBuilder headerBuilder = new StringBuilder();
            for (String field : fieldsToDisplay) {
                switch (field) {
                    case "姓名": headerBuilder.append(String.format("%-20s ", field)); break;
                    case "电话": headerBuilder.append(String.format("%-15s ", field)); break;
                    case "邮箱": headerBuilder.append(String.format("%-30s ", field)); break;
                    case "单位": headerBuilder.append(String.format("%-20s ", field)); break;
                    case "地址": headerBuilder.append(String.format("%-30s ", field)); break;
                    case "生日": headerBuilder.append(String.format("%-12s ", field)); break;
                }
            }
            groupMembersModel.addElement(headerBuilder.toString().trim());

            // 添加组成员
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                if (contact.getGroups().contains(currentSelectedGroup)) {
                    StringBuilder rowBuilder = new StringBuilder();
                    for (String field : fieldsToDisplay) {
                        switch (field) {
                            case "姓名":
                                rowBuilder.append(String.format("%-20s ", contact.getName()));
                                break;
                            case "电话":
                                rowBuilder.append(String.format("%-15s ",
                                    contact.getTelephone() != null ? contact.getTelephone() : "无"));
                                break;
                            case "邮箱":
                                rowBuilder.append(String.format("%-30s ",
                                    contact.getEmail() != null ? contact.getEmail() : "无"));
                                break;
                            case "单位":
                                rowBuilder.append(String.format("%-20s ",
                                    contact.getCompany() != null ? contact.getCompany() : "无"));
                                break;
                            case "地址":
                                rowBuilder.append(String.format("%-30s ",
                                    contact.getAddress() != null ? contact.getAddress() : "无"));
                                break;
                            case "生日":
                                rowBuilder.append(String.format("%-12s ",
                                    contact.getBirthday() != null ? contact.getBirthday() : "无"));
                                break;
                        }
                    }
                    groupMembersModel.addElement(rowBuilder.toString().trim());
                }
            }

            // 更新标题
            updateGroupMembersTitle();
        } catch (Exception e) {
            System.err.println("更新组成员列表时出错: " + e.getMessage());
        }
    }

    private void updateGroupMembersTitle() {
        if (groupMembersPanel != null) {
            Border border = groupMembersPanel.getBorder();
            if (border instanceof TitledBorder) {
                ((TitledBorder) border).setTitle("组成员 - " + currentSelectedGroup);
                groupMembersPanel.revalidate();
                groupMembersPanel.repaint();
            }
        }
    }

    private void clearSelectionsExcept(JList<?> selectedList) {
        if (selectedList != contactList) {
            contactList.clearSelection();
        }
        if (selectedList != groupList) {
            groupList.clearSelection();
        }
        if (selectedList != groupMembersList) {
            groupMembersList.clearSelection();
        }
    }

    private void showContactInfo(String name) {
        personalInfo contact = getContactInfo(name);
        if (contact != null) {
            // 创建详细信息面板
            JPanel detailPanel = new JPanel(new BorderLayout(10, 10));
            detailPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // 信息区域
            JTextArea detailArea = new JTextArea(contact.toString());
            detailArea.setEditable(false);
            detailArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            detailArea.setBackground(new Color(250, 250, 255));
            detailArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // 如果有照片，显示照片
            if (contact.getPhoto() != null && !contact.getPhoto().isEmpty()) {
                File photoFile = new File(contact.getPhoto());
                if (photoFile.exists()) {
                    try {
                        ImageIcon imageIcon = new ImageIcon(contact.getPhoto());
                        Image image = imageIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                        JLabel photoLabel = new JLabel(new ImageIcon(image));
                        photoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
                        detailPanel.add(photoLabel, BorderLayout.WEST);
                    } catch (Exception ex) {
                        System.err.println("加载照片失败: " + ex.getMessage());
                    }
                }
            }

            detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

            // 显示对话框
            JOptionPane.showMessageDialog(this,
                    detailPanel,
                    "联系人详细信息 - " + contact.getName(),
                    JOptionPane.PLAIN_MESSAGE);
        }
    }

    private void enableEditButton(boolean enable) {
        editButton.setEnabled(enable);
    }

    // 自定义滚动条UI
    static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(180, 190, 200);
            this.trackColor = new Color(240, 240, 245);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }


    }

    private void showMoveToGroupDialog() {
        List<String> selectedNames = getSelectedContactNames();
        if (selectedNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一个或多个联系人！");
            return;
        }

        // 获取第一个选中联系人的当前分组（用于默认选中）
        Set<String> currentGroups = new HashSet<>();
        try {
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                if (contact.getName().equals(selectedNames.get(0))) {
                    currentGroups = contact.getGroups();
                    break;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "加载联系人信息失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 创建对话框面板
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 添加说明标签
        JLabel label = new JLabel("<html><b>移动到分组</b><br>将删除原有分组，只保留新选中的分组:</html>");
        label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        panel.add(label, BorderLayout.NORTH);

        // 创建分组选择列表
        JList<String> groupSelectionList = new JList<>(groupListModel);
        groupSelectionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 设置默认选中当前分组
        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < groupListModel.size(); i++) {
            if (currentGroups.contains(groupListModel.getElementAt(i))) {
                selectedIndices.add(i);
            }
        }
        int[] indicesArray = selectedIndices.stream().mapToInt(i -> i).toArray();
        groupSelectionList.setSelectedIndices(indicesArray);

        // 美化列表外观
        groupSelectionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(new Font("微软雅黑", Font.PLAIN, 13));
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    setBackground(new Color(70, 130, 180));
                    setForeground(Color.WHITE);
                }
                return this;
            }
        });

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(groupSelectionList);
        styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(250, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 创建复选框 - 是否保留原有分组
        JCheckBox keepOriginalCheckBox = new JCheckBox("保留原有分组（不删除原有分组）");
        keepOriginalCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        keepOriginalCheckBox.setSelected(false); // 默认不保留
        panel.add(keepOriginalCheckBox, BorderLayout.SOUTH);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton okButton = createStyledButton("确定", new Color(60, 179, 113));
        JButton cancelButton = createStyledButton("取消", new Color(205, 92, 92));

        okButton.addActionListener(e -> {
            List<String> selectedGroups = groupSelectionList.getSelectedValuesList();
            if (selectedGroups.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请至少选择一个分组！", "提示", JOptionPane.WARNING_MESSAGE);
            } else {
                // 根据复选框决定是替换还是添加分组
                boolean replaceGroups = !keepOriginalCheckBox.isSelected();
                updateContactGroups(selectedNames, selectedGroups, replaceGroups);
                ((Window)SwingUtilities.getRoot(panel)).dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            ((Window)SwingUtilities.getRoot(panel)).dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 创建并显示对话框
        JDialog dialog = new JDialog(this, "移动到分组", true);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    private void updateContactGroups(List<String> contactNames, List<String> newGroups, boolean replaceExisting) {
        try {
            List<personalInfo> contacts = personalDao.loadAll();
            List<personalInfo> toUpdate = new ArrayList<>();

            for (personalInfo contact : contacts) {
                if (contactNames.contains(contact.getName())) {
                    Set<String> updatedGroups;

                    if (replaceExisting) {
                        // 替换模式：完全用新分组替换原有分组
                        updatedGroups = new HashSet<>(newGroups);

                        // 确保至少有一个分组
                        if (updatedGroups.isEmpty()) {
                            updatedGroups.add("未分组");
                        }
                    } else {
                        // 添加模式：保留原有分组并添加新分组
                        updatedGroups = new HashSet<>(contact.getGroups());
                        updatedGroups.addAll(newGroups);
                    }

                    // 更新联系人的分组
                    contact.setGroups(updatedGroups);
                    toUpdate.add(contact);
                }
            }

            // 批量更新联系人
            for (personalInfo contact : toUpdate) {
                personalDao.update(contact);
            }

            // 刷新界面
            loadContacts();
            loadGroupList(); // 重新加载分组列表
            updateGroupMembersList(); // 更新组成员列表

            JOptionPane.showMessageDialog(this,
                    "成功更新 " + toUpdate.size() + " 个联系人的分组信息",
                    "操作成功",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "更新分组失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeFromGroup() {
        List<String> selectedNames = getSelectedContactNames();
        if (selectedNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一个或多个联系人！");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要将选中的 " + selectedNames.size() + " 个联系人从当前分组移除吗？",
            "确认移除",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                List<personalInfo> contacts = personalDao.loadAll();
                List<personalInfo> toUpdate = new ArrayList<>();

                for (personalInfo contact : contacts) {
                    if (selectedNames.contains(contact.getName())) {
                        // 从当前分组移除
                        contact.removeGroup(currentSelectedGroup);
                        // 确保至少保留在"未分组"中
                        if (contact.getGroups().isEmpty()) {
                            contact.addGroup("未分组");
                        }
                        toUpdate.add(contact);
                    }
                }

                for (personalInfo contact : toUpdate) {
                    personalDao.update(contact);
                }

                loadContacts();
                updateGroupMembersList();
                JOptionPane.showMessageDialog(this,
                    "成功更新 " + toUpdate.size() + " 个联系人的分组信息");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "更新分组失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private List<String> getSelectedContactNames() {
        List<String> names = new ArrayList<>();

        if (!contactList.isSelectionEmpty()) {
            names.addAll(contactList.getSelectedValuesList());
        }

        if (!groupMembersList.isSelectionEmpty()) {
            for (String value : groupMembersList.getSelectedValuesList()) {
                if (!value.startsWith("姓名")) {
                    names.add(value.split(" ")[0]);
                }
            }
        }

        return names;
    }

    // 新增方法：显示属性设置对话框
    private void showDisplaySettingsDialog() {
        // 获取当前可用的属性列表
        String[] availableFields = {"姓名", "电话", "邮箱", "单位", "地址", "生日"};

        // 创建对话框面板
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 添加说明标签
        JLabel label = new JLabel("<html><b>选择要在组成员列表中显示的属性:</b></html>");
        label.setFont(new Font("微软雅黑", Font.BOLD, 13));
        panel.add(label, BorderLayout.NORTH);

        // 创建属性选择列表
        JList<String> fieldsList = new JList<>(availableFields);
        fieldsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 设置默认选中的属性（从配置或默认值）
        List<Integer> defaultSelected = Arrays.asList(0, 1, 2); // 默认显示姓名、电话、邮箱
        int[] selectedIndices = defaultSelected.stream().mapToInt(i -> i).toArray();
        fieldsList.setSelectedIndices(selectedIndices);

        // 美化列表外观
        fieldsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(new Font("微软雅黑", Font.PLAIN, 13));
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    setBackground(new Color(70, 130, 180));
                    setForeground(Color.WHITE);
                }
                return this;
            }
        });

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(fieldsList);
        styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(250, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton okButton = createStyledButton("确定", new Color(60, 179, 113));
        JButton cancelButton = createStyledButton("取消", new Color(205, 92, 92));

        okButton.addActionListener(e -> {
            List<String> selectedFields = fieldsList.getSelectedValuesList();
            if (selectedFields.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请至少选择一个属性！", "提示", JOptionPane.WARNING_MESSAGE);
            } else {
                // 保存设置并刷新显示
                saveDisplaySettings(selectedFields);
                updateGroupMembersList();
                ((Window)SwingUtilities.getRoot(panel)).dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            ((Window)SwingUtilities.getRoot(panel)).dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 创建并显示对话框
        JDialog dialog = new JDialog(this, "显示设置", true);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveDisplaySettings(List<String> selectedFields) {
        this.displayFields = selectedFields.toArray(new String[0]);
    }

    private void showImportDialog() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 格式选择
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formatPanel.add(new JLabel("导入格式:"));
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"CSV", "vCard"});
        styleComboBox(formatCombo);
        formatPanel.add(formatCombo);

        // 重复处理选项
        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JCheckBox skipDuplicatesCheck = new JCheckBox("跳过重复联系人");
        skipDuplicatesCheck.setSelected(true);
        optionPanel.add(skipDuplicatesCheck);

        // 文件选择
        JPanel filePanel = new JPanel(new BorderLayout(10, 10));
        JTextField fileField = new JTextField();
        styleTextField(fileField);
        JButton browseButton = createStyledButton("浏览...", new Color(70, 130, 180));

        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int selectedFormat = formatCombo.getSelectedIndex();
            if (selectedFormat == 0) {
                fileChooser.setFileFilter(new FileNameExtensionFilter("CSV文件", "csv"));
            } else {
                fileChooser.setFileFilter(new FileNameExtensionFilter("vCard文件", "vcf"));
            }

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);

        panel.add(formatPanel, BorderLayout.NORTH);
        panel.add(filePanel, BorderLayout.CENTER);
        panel.add(optionPanel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "导入联系人",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String filePath = fileField.getText();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请选择要导入的文件");
                return;
            }

            try {
                boolean skipDuplicates = skipDuplicatesCheck.isSelected();
                if (formatCombo.getSelectedIndex() == 0) {
                    personalDao.importFromCSV(filePath, skipDuplicates);
                } else {
                    personalDao.importFromVCard(filePath, skipDuplicates);
                }

                loadContacts();
                JOptionPane.showMessageDialog(this, "导入成功!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "导入失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 显示导出对话框
    private void showExportDialog() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 格式选择
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formatPanel.add(new JLabel("导出格式:"));
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"CSV", "vCard"});
        styleComboBox(formatCombo);
        formatPanel.add(formatCombo);

        // 导出范围选择
        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        ButtonGroup scopeGroup = new ButtonGroup();
        JRadioButton allContactsRadio = new JRadioButton("所有联系人", true);
        JRadioButton selectedContactsRadio = new JRadioButton("选中的联系人");
        JRadioButton currentGroupRadio = new JRadioButton("当前分组联系人");

        scopeGroup.add(allContactsRadio);
        scopeGroup.add(selectedContactsRadio);
        scopeGroup.add(currentGroupRadio);

        scopePanel.add(new JLabel("导出范围:"));
        scopePanel.add(allContactsRadio);
        scopePanel.add(selectedContactsRadio);
        scopePanel.add(currentGroupRadio);

        // 文件选择
        JPanel filePanel = new JPanel(new BorderLayout(10, 10));
        JTextField fileField = new JTextField();
        styleTextField(fileField);
        JButton browseButton = createStyledButton("浏览...", new Color(70, 130, 180));

        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int selectedFormat = formatCombo.getSelectedIndex();
            if (selectedFormat == 0) {
                fileChooser.setFileFilter(new FileNameExtensionFilter("CSV文件", "csv"));
            } else {
                fileChooser.setFileFilter(new FileNameExtensionFilter("vCard文件", "vcf"));
            }

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                // 确保文件扩展名正确
                if (selectedFormat == 0 && !filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                } else if (selectedFormat == 1 && !filePath.toLowerCase().endsWith(".vcf")) {
                    filePath += ".vcf";
                }
                fileField.setText(filePath);
            }
        });

        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);

        panel.add(formatPanel, BorderLayout.NORTH);
        panel.add(scopePanel, BorderLayout.CENTER);
        panel.add(filePanel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "导出联系人",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String filePath = fileField.getText();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请选择导出文件路径");
                return;
            }

            try {
                int exportScope = 0; // 0=所有, 1=选中, 2=当前分组
                if (selectedContactsRadio.isSelected()) exportScope = 1;
                else if (currentGroupRadio.isSelected()) exportScope = 2;

                List<personalInfo> contactsToExport = getContactsForExport(exportScope);

                if (contactsToExport.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "没有联系人可导出");
                    return;
                }

                if (formatCombo.getSelectedIndex() == 0) {
                    personalDao.exportToCSV(contactsToExport, filePath);
                } else {
                    personalDao.exportToVCard(contactsToExport, filePath);
                }

                JOptionPane.showMessageDialog(this, "成功导出 " + contactsToExport.size() + " 个联系人");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "导出失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 根据导出范围获取联系人列表
    private List<personalInfo> getContactsForExport(int exportScope) throws IOException {
        List<personalInfo> allContacts = personalDao.loadAll();

        if (exportScope == 0) { // 所有联系人
            return allContacts;
        } else if (exportScope == 1) { // 选中的联系人
            List<String> selectedNames = getSelectedContactNames();
            List<personalInfo> selectedContacts = new ArrayList<>();

            for (personalInfo contact : allContacts) {
                if (selectedNames.contains(contact.getName())) {
                    selectedContacts.add(contact);
                }
            }

            return selectedContacts;
        } else { // 当前分组联系人
            List<personalInfo> groupContacts = new ArrayList<>();

            for (personalInfo contact : allContacts) {
                if (contact.getGroups().contains(currentSelectedGroup)) {
                    groupContacts.add(contact);
                }
            }

            return groupContacts;
        }
    }
}