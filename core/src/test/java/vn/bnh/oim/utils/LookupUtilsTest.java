package vn.bnh.oim.utils;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcInvalidValueException;
import org.junit.Test;

import javax.security.auth.login.LoginException;
import java.text.Normalizer;

public class LookupUtilsTest {
    String hostname = "10.10.11.54";
    String port = "14000";
    String username = "xelsysadm";
    String passwd = "oracle_4U";
    String lookupTable = "Lookup.GenericRest.Groups";
    String lookupKey = "key";
    String lookupValue = "value";

    @Test
    public void importLookup() throws tcInvalidLookupException, tcInvalidValueException, tcAPIException, LoginException {
        OIMUtils.localInitialize(hostname, port, username, passwd);

        String[] ets = new String[]{"ABBANK.HCM,ABBANK An Giang", "ABBANK.HCM,ABBANK Bình Dương", "ABBANK.HCM,ABBANK An Nghiệp", "ABBANK.HCM,ABBANK Cần Thơ", "ABBANK.HNI,ABBANK Đà Nẵng", "ABBANK.HNI,ABBANK Hùng Vương", "ABBANK.HCM,ABBANK Đồng Nai", "ABBANK.HCM,ABBANK Đồng Tháp", "ABBANK.HNI,ABBANK Hồ Gươm", "ABBANK.HNI,ABBANK Hà Nội", "ABBANK.HNI,ABBANK Nguyễn Văn Cừ", "ABBANK.HNI,ABBANK Đống Đa", "ABBANK.HNI,ABBANK Trần Đăng Ninh", "ABBANK.HNI,ABBANK Trần Khát Chân", "ABBANK.HCM,ABBANK Bình Tân", "ABBANK.HCM,ABBANK Khánh Hội", "ABBANK.HCM,ABBANK Cộng Hòa", "ABBANK.HCM,ABBANK Thành Đô", "ABBANK.HCM,ABBANK Phú Mỹ Hưng", "ABBANK.HCM,ABBANK Dân Sinh", "ABBANK.HCM,ABBANK Lũy Bán Bích", "ABBANK.HCM,ABBANK Đakao", "ABBANK.HCM,ABBANK Chợ Lớn", "ABBANK.HCM,ABBANK Hậu Giang", "ABBANK.HCM,ABBANK Tân Phú", "ABBANK.HCM,ABBANK Quang Trung", "ABBANK.HCM,ABBANK Long An", "ABBANK.HCM,ABBANK Sài Gòn", "ABBANK.HCM,ABBANK Dân Chủ", "ABBANK.HCM,ABBANK Vĩnh Long", "ABBANK.HCM,ABBANK Bà Rịa Vũng Tàu", "ABBANK.HNI,ABBANK Quán Thánh", "ABBANK.HNI,ABBANK Lê Trọng Tấn", "ABBANK.HNI,ABBANK ABBANK.HNI Ninh", "ABBANK.HNI,ABBANK Quảng ABBANK.HCM", "ABBANK.HNI,ABBANK Thừa Thiên Huế", "ABBANK.HCM,ABBANK ABBANK.HNI Sài Gòn", "ABBANK.HCM,ABBANK Gia Định", "ABBANK.HCM,ABBANK Trường Chinh", "ABBANK.HCM,ABBANK Bình Phước", "ABBANK.HCM,ABBANK Kỳ Hòa", "ABBANK.HCM,ABBANK Lê Văn Việt", "ABBANK.HCM,ABBANK Phú Nhuận", "ABBANK.HNI,ABBANK Trưng Nữ Vương", "ABBANK.HCM,ABBANK Bình Thuận", "ABBANK.HCM,ABBANK Tây Ninh", "ABBANK.HNI,ABBANK Hưng Yên", "ABBANK.HNI,ABBANK Hà Đông", "ABBANK.HNI,ABBANK Phố Huế", "ABBANK.HNI,ABBANK Thái Nguyên", "ABBANK.HCM,ABBANK Tp.HCM", "ABBANK.HCM,ABBANK Kiên Giang", "ABBANK.HNI,ABBANK ABBANK.HCM Đà Nẵng", "ABBANK.HCM,ABBANK Minh Khai", "ABBANK.HCM,ABBANK Đông Bến Thành", "ABBANK.HCM,ABBANK Tiền Giang", "ABBANK.HNI,ABBANK Vĩnh Phúc", "ABBANK.HCM,ABBANK Tây Sài Gòn", "ABBANK.HNI,ABBANK Đội Cấn", "ABBANK.HCM,ABBANK Phó Cơ Điều", "ABBANK.HCM,ABBANK Bình Thủy", "ABBANK.HCM,ABBANK Vũng Tàu", "ABBANK.HCM,ABBANK Bà Rịa", "ABBANK.HCM,ABBANK Bến Cát", "ABBANK.HNI,ABBANK Sơn La", "ABBANK.HCM,ABBANK Bạc Liêu", "ABBANK.HNI,ABBANK Lò Đúc", "ABBANK.HCM,ABBANK Thuận An", "ABBANK.HNI,ABBANK Quảng Ninh", "ABBANK.HCM,ABBANK Khánh Hòa", "ABBANK.HNI,ABBANK Gia Lai", "ABBANK.HNI,ABBANK Hải Phòng", "ABBANK.HCM,ABBANK Cái Răng", "ABBANK.HNI,ABBANK Đào Tấn", "ABBANK.HNI,ABBANK Sông Hàn", "ABBANK.HNI,ABBANK Đinh Tiên Hoàng", "ABBANK.HNI,ABBANK Đông Anh", "ABBANK.HCM,ABBANK Chánh Hưng", "ABBANK.HNI,ABBANK Đại Kim", "ABBANK.HNI,ABBANK Mỹ Đình", "ABBANK.HCM,ABBANK Tân Bình", "ABBANK.HCM,ABBANK Phú Giáo", "ABBANK.HCM,ABBANK Thống Nhất", "ABBANK.HNI,ABBANK Mai Sơn", "ABBANK.HNI,ABBANK Hải Châu", "ABBANK.HNI,ABBANK Cẩm Phả", "ABBANK.HNI,ABBANK Liên Chiểu", "ABBANK.HCM,ABBANK Bến Nghé", "ABBANK.HNI,ABBANK Ngô Quyền", "ABBANK.HCM,ABBANK Tân Uyên", "ABBANK.HCM,ABBANK Nha Trang", "ABBANK.HCM,ABBANK Tân Thuận", "ABBANK.HNI,ABBANK Quang Trung - Hà Đông", "ABBANK.HCM,ABBANK Dầu Tiếng", "ABBANK.HCM,ABBANK Trung Chánh", "ABBANK.HCM,ABBANK Soái Kình Lâm", "ABBANK.HNI,ABBANK Yên Phong", "ABBANK.HNI,ABBANK Tây Hồ", "ABBANK.HNI,ABBANK Từ Sơn", "ABBANK.HNI,ABBANK Kbang", "ABBANK.HNI,ABBANK Đông Hà Nội", "ABBANK.HCM,ABBANK Bến Thành", "ABBANK.HCM,ABBANK Tô Hiến Thành", "ABBANK.HNI,ABBANK Mộc Châu", "ABBANK.HCM,ABBANK Chợ Biên Hòa", "ABBANK.HNI,ABBANK Uông Bí", "ABBANK.HCM,ABBANK Sóc Trăng", "ABBANK.HCM,ABBANK Bến Lức", "ABBANK.HCM,ABBANK Minh Phụng", "ABBANK.HNI,ABBANK Đông Ba", "ABBANK.HCM,ABBANK Tao Đàn", "ABBANK.HNI,ABBANK Tây Hà Nội", "ABBANK.HCM,ABBANK Bàn Cờ", "ABBANK.HNI,ABBANK Phố Hiến", "ABBANK.HNI,ABBANK Quyết Tâm", "ABBANK.HNI,ABBANK Quyết Thắng", "ABBANK.HNI,ABBANK Núi Thành", "ABBANK.HNI,ABBANK Hoàng Cầu", "ABBANK.HCM,ABBANK Long Thành", "ABBANK.HCM,ABBANK Cam Ranh", "ABBANK.HNI,ABBANK ABBANK.HCM Hà Nội", "ABBANK.HNI,ABBANK Nguyễn Chánh", "ABBANK.HNI,ABBANK ABBANK.HCM Thăng Long", "ABBANK.HNI,ABBANK Trung Yên", "ABBANK.HNI,ABBANK Vĩnh Yên", "ABBANK.HCM,ABBANK Hố Nai", "ABBANK.HNI,ABBANK Phổ Yên", "ABBANK.HNI,ABBANK Trần Nguyên Hãn", "ABBANK.HNI,ABBANK An Khánh", "ABBANK.HNI,ABBANK ABBANK.HNI Thăng Long", "ABBANK.HNI,ABBANK Nguyễn Huệ", "ABBANK.HNI,ABBANK Pleiku", "ABBANK.HNI,ABBANK Mỏ Bạch", "ABBANK.HCM,ABBANK Phước Tỉnh", "ABBANK.HNI,ABBANK Sốp Cộp", "ABBANK.HNI,ABBANK Phúc Yên", "ABBANK.HNI,ABBANK Mạo Khê", "ABBANK.HNI,ABBANK Văn Lâm", "ABBANK.HNI,ABBANK Đông Hải Phòng", "ABBANK.HCM,ABBANK Bình Long", "ABBANK.HNI,ABBANK Sông Mã", "ABBANK.HNI,ABBANK Xuân Hòa", "ABBANK.HNI,ABBANK Khoái Châu", "ABBANK.HNI,ABBANK Yên Lạc", "ABBANK.HCM,ABBANK Ba Mươi Tháng Tư", "ABBANK.HNI,ABBANK Hội An", "ABBANK.HNI,ABBANK Nghệ An", "ABBANK.HNI,ABBANK Lạng Sơn", "ABBANK.HNI,ABBANK Thanh Hóa", "ABBANK.HNI,ABBANK Thái Bình", "ABBANK.HNI,ABBANK Điện Biên", "ABBANK.HCM,ABBANK Gò Dầu", "ABBANK.HCM,ABBANK Chơn Thành", "ABBANK.HNI,ABBANK Chư Sê", "ABBANK.HCM,ABBANK Hồng Ngự", "ABBANK.HCM,ABBANK Lộc Ninh", "ABBANK.HCM,ABBANK Tân Châu", "ABBANK.HCM,ABBANK Vạn Ninh", "ABBANK.HNI,ABBANK Đại Từ", "ABBANK.HNI,ABBANK Thuận Thành", "ABBANK.HCM,ABBANK Đức Hòa", "ABBANK.HCM,ABBANK Tánh Linh", "ABBANK.HNI,ABBANK Phú Bài", "ABBANK.HNI,ABBANK ABBANK.HCM Phước", "ABBANK.HNI,ABBANK Đại Lộc"};
        for (String s : ets) {
            String newValue = s;
            if (s.contains("Đ") || s.contains("đ")) {
                newValue = newValue.replaceAll("Đ", "D").replaceAll("đ", "d");
            }
            newValue = Normalizer.normalize(newValue, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            LookupUtils.setLookupValue("Lookup.HRM.Users.Mapping.Company", newValue.split(",")[1], newValue.split(",")[0]);
        }
    }

    @Test
    public void addLookupValue() throws Exception {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        LookupUtils.setLookupValue("Lookup.GenericRest.Groups", "key", "value");
    }

    @Test
    public void getLookupValue() throws LoginException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        String output = LookupUtils.getLookupValue(lookupTable, lookupKey);
    }

    @Test
    public void removeLookupValue() throws LoginException, tcInvalidLookupException, tcInvalidValueException, tcAPIException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        LookupUtils.removeLookupEntryByKey("Lookup.GenericRest.Groups", "key");
    }

    @Test
    public void removeLookupValueByMeaning() throws LoginException, tcInvalidLookupException, tcAPIException, tcColumnNotFoundException, tcInvalidValueException {
        OIMUtils.localInitialize(hostname, port, username, passwd);
        LookupUtils.removeLookupEntryByValue(lookupTable, lookupValue);
    }

    @Test
    public void testParseDate() {
        String date = "20220819";
        System.out.println(date.substring(0, 4));
        System.out.println(date.substring(4, 6));
        System.out.println(date.substring(6));
    }
}