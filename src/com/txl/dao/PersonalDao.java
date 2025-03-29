package com.txl.dao;

import com.txl.dean.personalInfo;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

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
            escapeCsvField(info.getGroup()),
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

    // 将联系人对象转换为CSV行
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
            escapeCsvField(contact.getGroup()),
            escapeCsvField(contact.getNotes())
        );
    }

    // 从CSV行解析联系人对象
    private personalInfo parseContactFromCsvLine(String line) {
        try {
            List<String> fields = parseCSV(line);
            if (fields.size() < 11) { // 更新为新的字段数量
                return null; // 数据不完整
            }

            personalInfo contact = new personalInfo(fields.get(0));
            contact.setTelephone(fields.get(1));
            contact.setEmail(fields.get(2));
            contact.setHomepage(fields.get(3));
            contact.setBirthday(fields.get(4));
            contact.setPhoto(fields.get(5));
            contact.setCompany(fields.get(6));
            contact.setAddress(fields.get(7));
            contact.setZipCode(fields.get(8));
            contact.setGroup(fields.get(9));
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
}