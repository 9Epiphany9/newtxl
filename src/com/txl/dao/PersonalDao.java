package com.txl.dao;

import com.txl.dean.personalInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class PersonalDao {
    private static final String CSV_HEADER = "姓名,电话,邮箱,主页,生日,照片,单位,地址,邮编,分组,备注";
    private final String csvFilePath;

    public PersonalDao(String csvFilePath) {
        this.csvFilePath = csvFilePath;
        initCsvFileIfNotExists();
    }

    // 初始化CSV文件（如果不存在）
    private void initCsvFileIfNotExists() {
        File file = new File(csvFilePath);
        if (!file.exists()) {
            try {
                // 创建目录（如果不存在）
                File directory = file.getParentFile();
                if (directory != null && !directory.exists()) {
                    directory.mkdirs();
                }
                // 创建文件并写入表头
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println(CSV_HEADER);
                }
            } catch (IOException e) {
                throw new RuntimeException("无法创建CSV文件: " + e.getMessage());
            }
        }
    }

    // 将personalInfo对象添加到CSV文件
    public void add(personalInfo info) throws IOException {
        // 先检查是否已存在同名联系人，如果存在则更新
        List<personalInfo> contacts = loadAll();
        boolean exists = false;
        for (personalInfo contact : contacts) {
            if (contact.getName().equals(info.getName())) {
                exists = true;
                break;
            }
        }

        if (exists) {
            update(info); // 如果已存在，则更新
            return;
        }

        // 构建CSV行
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            escapeCsvField(info.getName()),
            escapeCsvField(info.getTelephone()),
            escapeCsvField(info.getEmail()),
            escapeCsvField(info.getHomepage()),
            escapeCsvField(info.getBirthday()),
            escapeCsvField(info.getPhoto()),
            escapeCsvField(info.getCompany()),
            escapeCsvField(info.getAddress()),
            escapeCsvField(info.getZipCode()),
            escapeCsvField(String.join(";", info.getGroups())), // 修改此行
            escapeCsvField(info.getNotes())
        );

        // 追加到文件
        try {
            Files.write(
                Paths.get(csvFilePath),
                (csvLine + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IOException("添加联系人信息失败: " + e.getMessage());
        }
    }

    // 加载所有联系人
    public List<personalInfo> loadAll() throws IOException {
        List<personalInfo> contacts = new ArrayList<>();
        File file = new File(csvFilePath);

        if (!file.exists()) {
            return contacts; // 文件不存在，返回空列表
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // 跳过表头
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue; // 跳过空行
                }

                personalInfo contact = parseContactFromCsvLine(line);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
        }

        return contacts;
    }

    // 更新联系人信息
    public void update(personalInfo info) throws IOException {
        List<personalInfo> contacts = loadAll();
        List<String> lines = new ArrayList<>();
        lines.add(CSV_HEADER); // 添加表头

        boolean found = false;

        // 替换或保留原有联系人
        for (personalInfo contact : contacts) {
            if (contact.getName().equals(info.getName())) {
                // 如果目标分组包含“未分组”，则移除所有其他分组
                if (info.getGroups().contains("未分组")) {
                    info.setGroups(new HashSet<>(Arrays.asList("未分组")));
                }
                // 添加更新后的联系人
                lines.add(contactToCsvLine(info));
                found = true;
            } else {
                // 保留其他联系人
                lines.add(contactToCsvLine(contact));
            }
        }

        // 如果没找到，添加为新联系人
        if (!found) {
            lines.add(contactToCsvLine(info));
        }

        // 重写整个文件
        Files.write(
            Paths.get(csvFilePath),
            lines,
            StandardCharsets.UTF_8
        );
    }

    // 删除联系人
    public void delete(String name) throws IOException {
        List<personalInfo> contacts = loadAll();
        List<String> lines = new ArrayList<>();
        lines.add(CSV_HEADER); // 添加表头

        // 保留非删除联系人
        for (personalInfo contact : contacts) {
            if (!contact.getName().equals(name)) {
                lines.add(contactToCsvLine(contact));
            }
        }

        // 重写整个文件
        Files.write(
            Paths.get(csvFilePath),
            lines,
            StandardCharsets.UTF_8
        );
    }

    private String contactToCsvLine(personalInfo contact) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            escapeCsvField(contact.getName()),
            escapeCsvField(contact.getTelephone()),
            escapeCsvField(contact.getEmail()),
            escapeCsvField(contact.getHomepage()),
            escapeCsvField(contact.getBirthday()),
            escapeCsvField(contact.getPhoto()),
            escapeCsvField(contact.getCompany()),
            escapeCsvField(contact.getAddress()),
            escapeCsvField(contact.getZipCode()),
            escapeCsvField(String.join(";", contact.getGroups())), // 修改此行
            escapeCsvField(contact.getNotes())
        );
    }

    private personalInfo parseContactFromCsvLine(String line) {
        try {
            List<String> fields = parseCSV(line);
            if (fields.size() < 11) return null;

            personalInfo contact = new personalInfo(fields.get(0));
            contact.setTelephone(fields.get(1));
            contact.setEmail(fields.get(2));
            contact.setHomepage(fields.get(3));
            contact.setBirthday(fields.get(4));
            contact.setPhoto(fields.get(5));
            contact.setCompany(fields.get(6));
            contact.setAddress(fields.get(7));
            contact.setZipCode(fields.get(8));
            
            // 修改分组解析逻辑
            Set<String> groups = new HashSet<>(Arrays.asList(fields.get(9).split(";")));
            contact.setGroups(groups);
            
            contact.setNotes(fields.get(10));

            return contact;
        } catch (Exception e) {
            System.err.println("解析CSV行失败: " + e.getMessage());
            return null;
        }
    }

    // 处理CSV字段中的特殊字符
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果字段包含逗号、换行符或双引号，需要用双引号包围
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            // 将字段中的双引号替换为两个双引号
            field = field.replace("\"", "\"\"");
            // 用双引号包围整个字段
            return "\"" + field + "\"";
        }
        return field;
    }

    // 解析CSV行，正确处理引号
    private List<String> parseCSV(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                // 如果当前字符是引号
                if (inQuotes) {
                    // 如果已经在引号内，检查下一个字符
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        // 如果下一个字符也是引号，这是转义的引号
                        sb.append('\"');
                        i++; // 跳过下一个字符
                    } else {
                        // 否则，这是引号结束
                        inQuotes = false;
                    }
                } else {
                    // 开始引号
                    inQuotes = true;
                }
            } else if (c == ',' && !inQuotes) {
                // 如果是逗号且不在引号内，添加字段并重置
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                // 普通字符，添加到当前字段
                sb.append(c);
            }
        }

        // 添加最后一个字段
        fields.add(sb.toString());

        return fields;
    }

    // 在PersonalDao类中添加以下方法

    // 导出全部联系人到CSV文件
    public void exportAllToCSV(String exportPath) throws IOException {
        List<personalInfo> contacts = loadAll();
        exportToCSV(contacts, exportPath);
    }

    // 导出指定联系人到CSV文件
    public void exportToCSV(List<personalInfo> contacts, String exportPath) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(CSV_HEADER);

        for (personalInfo contact : contacts) {
            lines.add(contactToCsvLine(contact));
        }

        Files.write(Paths.get(exportPath), lines, StandardCharsets.UTF_8);
    }

    // 从CSV文件导入联系人
    public void importFromCSV(String importPath, boolean skipDuplicates) throws IOException {
        List<personalInfo> existingContacts = loadAll();
        Set<String> existingNames = new HashSet<>();
        for (personalInfo contact : existingContacts) {
            existingNames.add(contact.getName());
        }

        List<personalInfo> newContacts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(importPath))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                personalInfo contact = parseContactFromCsvLine(line);
                if (contact != null) {
                    if (skipDuplicates && existingNames.contains(contact.getName())) {
                        continue;
                    }
                    newContacts.add(contact);
                }
            }
        }

        // 批量添加新联系人
        for (personalInfo contact : newContacts) {
            add(contact);
        }
    }

    // 导出全部联系人到vCard文件
    public void exportAllToVCard(String exportPath) throws IOException {
        List<personalInfo> contacts = loadAll();
        exportToVCard(contacts, exportPath);
    }

    // 导出指定联系人到vCard文件
    public void exportToVCard(List<personalInfo> contacts, String exportPath) throws IOException {
        StringBuilder vcardContent = new StringBuilder();

        for (personalInfo contact : contacts) {
            vcardContent.append(contactToVCard(contact)).append("\n");
        }

        Files.write(Paths.get(exportPath), vcardContent.toString().getBytes(StandardCharsets.UTF_8));
    }

    // 从vCard文件导入联系人
    public void importFromVCard(String importPath, boolean skipDuplicates) throws IOException {
        List<personalInfo> existingContacts = loadAll();
        Set<String> existingNames = new HashSet<>();
        for (personalInfo contact : existingContacts) {
            existingNames.add(contact.getName());
        }

        String vcardContent = new String(Files.readAllBytes(Paths.get(importPath)), StandardCharsets.UTF_8);
        String[] vcards = vcardContent.split("END:VCARD");

        List<personalInfo> newContacts = new ArrayList<>();

        for (String vcard : vcards) {
            if (vcard.trim().isEmpty()) continue;

            personalInfo contact = parseVCard(vcard + "END:VCARD");
            if (contact != null) {
                if (skipDuplicates && existingNames.contains(contact.getName())) {
                    continue;
                }
                newContacts.add(contact);
            }
        }

        // 批量添加新联系人
        for (personalInfo contact : newContacts) {
            add(contact);
        }
    }

    // 将联系人转换为vCard格式
    private String contactToVCard(personalInfo contact) {
        StringBuilder vcard = new StringBuilder();
        vcard.append("BEGIN:VCARD\n");
        vcard.append("VERSION:3.0\n");
        vcard.append("FN:").append(escapeVCardField(contact.getName())).append("\n");

        if (contact.getTelephone() != null && !contact.getTelephone().isEmpty()) {
            vcard.append("TEL:").append(contact.getTelephone()).append("\n");
        }

        if (contact.getEmail() != null && !contact.getEmail().isEmpty()) {
            vcard.append("EMAIL:").append(contact.getEmail()).append("\n");
        }

        if (contact.getBirthday() != null && !contact.getBirthday().isEmpty()) {
            vcard.append("BDAY:").append(contact.getBirthday()).append("\n");
        }

        if (contact.getCompany() != null && !contact.getCompany().isEmpty()) {
            vcard.append("ORG:").append(escapeVCardField(contact.getCompany())).append("\n");
        }

        if (contact.getAddress() != null && !contact.getAddress().isEmpty()) {
            vcard.append("ADR:;;").append(escapeVCardField(contact.getAddress())).append("\n");
        }

        if (contact.getNotes() != null && !contact.getNotes().isEmpty()) {
            vcard.append("NOTE:").append(escapeVCardField(contact.getNotes())).append("\n");
        }

        if (contact.getPhoto() != null && !contact.getPhoto().isEmpty()) {
            try {
                String photoBase64 = encodePhotoToBase64(contact.getPhoto());
                vcard.append("PHOTO;ENCODING=BASE64;TYPE=JPEG:").append(photoBase64).append("\n");
            } catch (IOException e) {
                System.err.println("无法编码照片: " + e.getMessage());
            }
        }

        vcard.append("END:VCARD");
        return vcard.toString();
    }

    // 解析vCard为联系人对象
    private personalInfo parseVCard(String vcard) {
        personalInfo contact = null;

        String[] lines = vcard.split("\n");
        for (String line : lines) {
            if (line.startsWith("FN:")) {
                String name = unescapeVCardField(line.substring(3).trim());
                contact = new personalInfo(name);
            } else if (contact != null) {
                if (line.startsWith("TEL:")) {
                    contact.setTelephone(line.substring(4).trim());
                } else if (line.startsWith("EMAIL:")) {
                    contact.setEmail(line.substring(6).trim());
                } else if (line.startsWith("BDAY:")) {
                    contact.setBirthday(line.substring(5).trim());
                } else if (line.startsWith("ORG:")) {
                    contact.setCompany(unescapeVCardField(line.substring(4).trim()));
                } else if (line.startsWith("ADR:;;")) {
                    contact.setAddress(unescapeVCardField(line.substring(5).trim()));
                } else if (line.startsWith("NOTE:")) {
                    contact.setNotes(unescapeVCardField(line.substring(5).trim()));
                }
            }
        }

        return contact;
    }

    // 编码照片为Base64
    private String encodePhotoToBase64(String photoPath) throws IOException {
        byte[] photoBytes = Files.readAllBytes(Paths.get(photoPath));
        return Base64.getEncoder().encodeToString(photoBytes);
    }

    // 转义vCard字段中的特殊字符
    private String escapeVCardField(String field) {
        if (field == null) return "";
        return field.replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;");
    }

    // 反转义vCard字段
    private String unescapeVCardField(String field) {
        if (field == null) return "";
        return field.replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";");
    }
}

