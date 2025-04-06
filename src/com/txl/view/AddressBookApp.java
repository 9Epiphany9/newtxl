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

    public AddressBookApp() {
        setTitle("通讯录管理系统");
        setSize(900, 600);
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
                contactGroupMap.put(name, contact.getGroup());
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

        // 组成员面板
        groupMembersPanel = createStyledPanel("组成员 - " + currentSelectedGroup, 450, 400);
        groupMembersPanel.setLayout(new BorderLayout());
        groupMembersModel = new DefaultListModel<>();
        groupMembersList = new JList<>(groupMembersModel);
        styleList(groupMembersList);
        groupMembersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    clearSelectionsExcept(groupMembersList);
                    String selectedName = groupMembersList.getSelectedValue();
                    if (selectedName != null && !selectedName.startsWith("姓名")) {
                        showContactInfo(selectedName.split(" ")[0]);
                    }
                }
            }
        });
        groupMembersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedName = groupMembersList.getSelectedValue();
                if (selectedName != null && !selectedName.startsWith("姓名")) {
                    clearSelectionsExcept(groupMembersList);
                    String name = selectedName.split(" ")[0];
                    for (int i = 0; i < contactListModel.size(); i++) {
                        if (contactListModel.get(i).equals(name)) {
                            contactList.setSelectedIndex(i);
                            break;
                        }
                    }
                    enableEditButton(true);
                } else {
                    enableEditButton(false);
                }
            }
        });

        JScrollPane groupMembersScrollPane = new JScrollPane(groupMembersList);
        styleScrollPane(groupMembersScrollPane);
        groupMembersPanel.add(groupMembersScrollPane, BorderLayout.CENTER);

        // 搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        searchField = new JTextField(25);
        styleTextField(searchField);

        JButton searchButton = createStyledButton("搜索", new Color(70, 130, 180));
        searchButton.addActionListener(e -> searchContacts());
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setOpaque(false);

        JButton addButton = createStyledButton("添加联系人", new Color(60, 179, 113));
        JButton deleteButton = createStyledButton("删除联系人", new Color(205, 92, 92));
        editButton = createStyledButton("编辑联系人", new Color(70, 130, 180));

        setupButtonListeners(addButton, deleteButton, editButton, searchButton);

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);

        // 组装右侧面板
        rightPanel.add(groupMembersPanel, BorderLayout.CENTER);
        rightPanel.add(searchPanel, BorderLayout.NORTH);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加到主面板
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        JButton moveToGroupButton = createStyledButton("移动到分组", new Color(70, 130, 180));
        JButton removeFromGroupButton = createStyledButton("从分组移除", new Color(205, 92, 92));

        moveToGroupButton.addActionListener(e -> showMoveToGroupDialog());
        removeFromGroupButton.addActionListener(e -> removeFromGroup());

        buttonPanel.add(moveToGroupButton);
        buttonPanel.add(removeFromGroupButton);
    }

    private void loadGroupList() {
        Set<String> groups = new HashSet<>();
        groups.add("未分组"); // 默认分组

        try {
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                if (contact.getGroup() != null && !contact.getGroup().isEmpty()) {
                    groups.add(contact.getGroup());
                }
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
                    if (selectedGroup.equals(contact.getGroup())) {
                        // 2. 将这些联系人的分组设置为"未分组"
                        contact.setGroup("未分组");
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

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK); // 固定使用黑色文字

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
            contact.setGroup((String) groupField.getSelectedItem());
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
        JComboBox<String> groupField = new JComboBox<>();
        for (int i = 0; i < groupListModel.size(); i++) {
            groupField.addItem(groupListModel.getElementAt(i));
        }
        groupField.setSelectedItem(originalContact.getGroup());

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
        addFormField(formPanel, "分组:", groupField);
        addFormField(formPanel, "备注:", notesField);

        panel.add(formPanel, BorderLayout.CENTER);

        // 显示对话框
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "编辑联系人",
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
            contact.setGroup((String) groupField.getSelectedItem());
            contact.setNotes(notesField.getText().trim());

            try {
                personalDao.update(contact);
                loadContacts();
                loadGroupList(); // 刷新分组列表
                JOptionPane.showMessageDialog(this, "联系人编辑成功！");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "保存联系人失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
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

        try {
            // 添加表头
            String header = String.format("%-20s %-15s %-30s", "姓名", "电话", "邮箱");
            groupMembersModel.addElement(header);

            // 添加组成员
            List<personalInfo> contacts = personalDao.loadAll();
            for (personalInfo contact : contacts) {
                if (currentSelectedGroup.equals(contact.getGroup())) {
                    String displayText = String.format("%-20s %-15s %-30s",
                            contact.getName(),
                            contact.getTelephone() != null ? contact.getTelephone() : "无",
                            contact.getEmail() != null ? contact.getEmail() : "无");
                    groupMembersModel.addElement(displayText);
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

        JComboBox<String> groupComboBox = new JComboBox<>();
        for (int i = 0; i < groupListModel.size(); i++) {
            groupComboBox.addItem(groupListModel.getElementAt(i));
        }
        styleComboBox(groupComboBox);

        int result = JOptionPane.showConfirmDialog(
                this,
                groupComboBox,
                "选择目标分组",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String targetGroup = (String) groupComboBox.getSelectedItem();
            if (targetGroup != null) {
                moveContactsToGroup(selectedNames, targetGroup);
            }
        }
    }

    private void moveContactsToGroup(List<String> names, String targetGroup) {
        try {
            List<personalInfo> contacts = personalDao.loadAll();
            List<personalInfo> toUpdate = new ArrayList<>();

            for (personalInfo contact : contacts) {
                if (names.contains(contact.getName())) {
                    contact.setGroup(targetGroup);
                    toUpdate.add(contact);
                }
            }

            for (personalInfo contact : toUpdate) {
                personalDao.update(contact);
            }

            loadContacts();
            updateGroupMembersList();
            JOptionPane.showMessageDialog(this,
                    "成功将 " + toUpdate.size() + " 个联系人移动到分组: " + targetGroup);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "移动联系人失败: " + ex.getMessage(),
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
            moveContactsToGroup(selectedNames, "未分组");
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            AddressBookApp app = new AddressBookApp();
            app.setVisible(true);
        });
    }
}