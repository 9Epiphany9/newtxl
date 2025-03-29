package com.txl.view;
import com.txl.dao.PersonalDao;
import com.txl.dean.personalInfo;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressBookApp extends JFrame {
    private static final String DATA_DIR = "data";
    private static final String CONTACTS_FILE = DATA_DIR + File.separator + "contacts.csv";

    private DefaultListModel<String> contactListModel; // 联系人列表数据模型
    private JList<String> contactList; // 联系人列表
    private JComboBox<String> filterComboBox; // 过滤选项（所有联系人、未分组联系人）
    private DefaultListModel<String> groupListModel; // 联系组列表数据模型
    private JList<String> groupList; // 联系组列表
    private JComboBox<String> groupComboBox; // 分组选择框
    private JTextField searchField; // 搜索框

    // 右侧组成员列表
    private DefaultListModel<String> groupMembersModel; // 组成员列表数据模型
    private JList<String> groupMembersList; // 组成员列表

    // 存储联系人及其分组信息
    private Map<String, String> contactGroupMap = new HashMap<>();

    // 添加PersonalDao实例
    private PersonalDao personalDao;

    public AddressBookApp() {
        setTitle("通讯录管理系统");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 窗口居中

        // 确保数据目录存在
        new File(DATA_DIR).mkdirs();

        // 初始化PersonalDao
        personalDao = new PersonalDao(CONTACTS_FILE);

        initUI(); // 初始化界面
        loadContacts(); // 加载已存储的联系人
    }

    private void loadContacts() {
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
        // ... [保持其他UI初始化代码不变，直到setupButtonListeners调用] ...

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel);

        // 左侧面板：联系人列表和联系组
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(300, 500));

        // 联系人列表面板
        JPanel contactPanel = new JPanel(new BorderLayout());
        contactPanel.setBorder(createTitledBorder("联系人"));

        // 过滤选项（所有联系人、未分组联系人）
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterComboBox = new JComboBox<>(new String[]{"所有联系人", "未分组联系人"});
        filterComboBox.addActionListener(e -> filterContacts());
        filterPanel.add(new JLabel("显示："));
        filterPanel.add(filterComboBox);
        contactPanel.add(filterPanel, BorderLayout.NORTH);

        // 联系人列表
        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        contactList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedName = contactList.getSelectedValue();
                    if (selectedName != null) {
                        personalInfo contact = getContactInfo(selectedName);
                        if (contact != null) {
                            // 创建详细信息面板
                            JPanel detailPanel = new JPanel(new BorderLayout());
                            JTextArea detailArea = new JTextArea(contact.toString());
                            detailArea.setEditable(false);
                            detailArea.setBackground(new Color(245, 245, 245));
                            detailArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                            // 如果有照片，显示照片
                            if (contact.getPhoto() != null && !contact.getPhoto().isEmpty()) {
                                File photoFile = new File(contact.getPhoto());
                                if (photoFile.exists()) {
                                    try {
                                        ImageIcon imageIcon = new ImageIcon(contact.getPhoto());
                                        // 调整图片大小
                                        Image image = imageIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                                        JLabel photoLabel = new JLabel(new ImageIcon(image));
                                        detailPanel.add(photoLabel, BorderLayout.WEST);
                                    } catch (Exception ex) {
                                        System.err.println("加载照片失败: " + ex.getMessage());
                                    }
                                }
                            }

                            detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

                            // 显示详细信息
                            JOptionPane.showMessageDialog(AddressBookApp.this,
                                detailPanel,
                                "联系人详细信息",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });
        JScrollPane listScrollPane = new JScrollPane(contactList);
        contactPanel.add(listScrollPane, BorderLayout.CENTER);

        // 将联系人面板添加到左侧
        leftPanel.add(contactPanel, BorderLayout.CENTER);

        // 联系组列表
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        // 添加联系组选择监听器
        groupList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateGroupMembersList();
                }
            }
        });
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setBorder(createTitledBorder("联系组"));
        leftPanel.add(groupScrollPane, BorderLayout.SOUTH);

        // 右侧面板：功能区域
        JPanel rightPanel = new JPanel(new BorderLayout());

        // 分组选择框
        JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        groupPanel.setBorder(createTitledBorder("分组"));
        groupComboBox = new JComboBox<>(new String[]{"未分组", "家人", "朋友", "同事"});
        groupPanel.add(groupComboBox);
        rightPanel.add(groupPanel, BorderLayout.NORTH);

        // 右侧中部面板（包含组成员列表和搜索区域）
        JPanel rightCenterPanel = new JPanel(new BorderLayout());

        // 组成员列表面板
        JPanel groupMembersPanel = new JPanel(new BorderLayout());
        groupMembersPanel.setBorder(createTitledBorder("组成员"));
        groupMembersModel = new DefaultListModel<>();
        groupMembersList = new JList<>(groupMembersModel);
        JScrollPane groupMembersScrollPane = new JScrollPane(groupMembersList);
        groupMembersScrollPane.setPreferredSize(new Dimension(400, 250));
        groupMembersPanel.add(groupMembersScrollPane, BorderLayout.CENTER);
        rightCenterPanel.add(groupMembersPanel, BorderLayout.CENTER);

        // 搜索框
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("搜索");
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        rightCenterPanel.add(searchPanel, BorderLayout.SOUTH);

        // 将中部面板添加到右侧面板
        rightPanel.add(rightCenterPanel, BorderLayout.CENTER);

        // 操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addButton = new JButton("添加联系人");
        JButton deleteButton = new JButton("删除联系人");
        JButton editButton = new JButton("编辑联系人");
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(editButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 将左右面板添加到主面板
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        // 按钮事件监听
        setupButtonListeners(addButton, deleteButton, editButton, searchButton);

        // 初始化联系组
        groupListModel.addElement("未分组");
        groupListModel.addElement("家人");
        groupListModel.addElement("朋友");
        groupListModel.addElement("同事");
    }

    /**
     * 创建带标题的边框
     */
    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("宋体", Font.BOLD, 14),
                Color.BLACK
        );
    }

    /**
     * 设置按钮事件监听
     */
    private void setupButtonListeners(JButton addButton, JButton deleteButton, JButton editButton, JButton searchButton) {
        // 添加联系人
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 创建联系人信息输入面板
                JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                // 创建输入字段
                JTextField nameField = new JTextField(20);
                JTextField telephoneField = new JTextField(20);
                JTextField emailField = new JTextField(20);
                JComboBox<String> groupField = new JComboBox<>(new String[]{"未分组", "家人", "朋友", "同事"});

                // 添加字段到面板
                panel.add(new JLabel("姓名 *:"));
                panel.add(nameField);
                panel.add(new JLabel("电话:"));
                panel.add(telephoneField);
                panel.add(new JLabel("邮箱:"));
                panel.add(emailField);
                panel.add(new JLabel("分组:"));
                panel.add(groupField);

                // 显示对话框
                int result = JOptionPane.showConfirmDialog(
                    AddressBookApp.this,
                    panel,
                    "添加联系人",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                );

                // 处理用户输入
                if (result == JOptionPane.OK_OPTION) {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) {
                        JOptionPane.showMessageDialog(AddressBookApp.this, "姓名不能为空！");
                        return;
                    }

                    // 创建新的personalInfo对象
                    personalInfo contact = new personalInfo(name);
                    contact.setTelephone(telephoneField.getText().trim());
                    contact.setEmail(emailField.getText().trim());
                    contact.setGroup((String) groupField.getSelectedItem());

                    try {
                        // 使用PersonalDao保存到CSV文件
                        personalDao.add(contact);

                        // 更新UI
                        contactListModel.addElement(name);
                        contactGroupMap.put(name, contact.getGroup());
                        updateGroupMembersList();

                        JOptionPane.showMessageDialog(AddressBookApp.this, "联系人添加成功！");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(AddressBookApp.this,
                            "保存联系人失败: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // ... [保持其他按钮监听器代码不变] ...
        // 删除联系人
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = contactList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String contact = contactListModel.get(selectedIndex);
                    contactListModel.remove(selectedIndex);
                    contactGroupMap.remove(contact); // 从分组中移除
                } else {
                    JOptionPane.showMessageDialog(AddressBookApp.this, "请先选择一个联系人！");
                }
            }
        });

        // 编辑联系人
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = contactList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String currentName = contactList.getSelectedValue();
                    String newName = JOptionPane.showInputDialog(AddressBookApp.this, "编辑联系人姓名：", currentName);
                    if (newName != null && !newName.trim().isEmpty()) {
                        String group = contactGroupMap.get(currentName);
                        contactListModel.set(selectedIndex, newName);
                        contactGroupMap.remove(currentName);
                        contactGroupMap.put(newName, group); // 更新分组信息
                    }
                } else {
                    JOptionPane.showMessageDialog(AddressBookApp.this, "请先选择一个联系人！");
                }
            }
        });

        // 搜索联系人
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String keyword = searchField.getText().trim().toLowerCase();
                if (!keyword.isEmpty()) {
                    try {
                        // 获取所有联系人详细信息
                        List<personalInfo> contacts = personalDao.loadAll();
                        DefaultListModel<String> filteredModel = new DefaultListModel<>();

                        // 在所有字段中搜索关键词
                        for (personalInfo contact : contacts) {
                            if (contact.getName().toLowerCase().contains(keyword) ||
                                (contact.getTelephone() != null && contact.getTelephone().toLowerCase().contains(keyword)) ||
                                (contact.getEmail() != null && contact.getEmail().toLowerCase().contains(keyword)) ||
                                (contact.getCompany() != null && contact.getCompany().toLowerCase().contains(keyword)) ||
                                (contact.getAddress() != null && contact.getAddress().toLowerCase().contains(keyword)) ||
                                (contact.getGroup() != null && contact.getGroup().toLowerCase().contains(keyword))) {

                                filteredModel.addElement(contact.getName());
                            }
                        }
                        contactList.setModel(filteredModel);

                        // 显示搜索结果数量
                        String message = String.format("找到 %d 个匹配的联系人", filteredModel.getSize());
                        JOptionPane.showMessageDialog(AddressBookApp.this, message);

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(AddressBookApp.this,
                            "搜索联系人失败: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                        contactList.setModel(contactListModel); // 发生错误时恢复完整列表
                    }
                } else {
                    contactList.setModel(contactListModel); // 恢复完整列表
                }
            }
        });

        // 分组选择框事件
        groupComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = contactList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String contact = contactListModel.get(selectedIndex);
                    String group = (String) groupComboBox.getSelectedItem();
                    contactGroupMap.put(contact, group); // 更新分组信息
                    filterContacts(); // 刷新联系人列表
                    updateGroupMembersList(); // 刷新组成员列表
                    groupComboBox.setSelectedItem("未分组"); // 重置为未分组
                }
            }
        });
    }

    /**
     * 过滤联系人列表
     */
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

    /**
     * 更新组成员列表
     */
    /**
     * 获取联系人的完整信息
     * @param name 联系人姓名
     * @return 联系人信息对象，如果未找到返回null
     */
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

    /**
     * 更新组成员列表
     */
    private void updateGroupMembersList() {
        String selectedGroup = groupList.getSelectedValue();
        if (selectedGroup != null) {
            groupMembersModel.clear();

            try {
                JPanel mainPanel = (JPanel) getContentPane();
                JPanel rightPanel = (JPanel) mainPanel.getComponent(1);
                JPanel rightCenterPanel = (JPanel) rightPanel.getComponent(1);
                JPanel groupMembersPanel = (JPanel) rightCenterPanel.getComponent(0);

                // 更新标题
                TitledBorder border = (TitledBorder) groupMembersPanel.getBorder();
                border.setTitle("组成员 - " + selectedGroup);

                // 添加所有属于该组的联系人
                for (Map.Entry<String, String> entry : contactGroupMap.entrySet()) {
                    if (selectedGroup.equals(entry.getValue())) {
                        groupMembersModel.addElement(entry.getKey());
                    }
                }

                // 重绘组成员面板
                groupMembersPanel.repaint();
            } catch (Exception e) {
                System.err.println("更新组成员列表时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            // 设置本地系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AddressBookApp().setVisible(true);
            }
        });
    }
}