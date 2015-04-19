/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;

/**
 *
 * @author Administrator
 */
public class Tools {

    public final static Pattern NUMBER_PATTERN = Pattern.compile("\\d*");

    public static boolean isAllNumber(String s) {
        if (s == null) {
            return true;
        }

        Matcher m = NUMBER_PATTERN.matcher(s);
        if (m.matches()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean matchString(String s, String pattern) {
        boolean b = Pattern.matches(pattern, s);
        return b;
    }

    /**
     * 根据lunh算法生成卡号
     * @param s
     * @return
     */
    public static String generateBankCardNum(String s) {
        if (s == null) {
            return "";
        }

        int sum = 0;
        int num = 0;
        int len = s.length();

        for (int i = 0; i < len; i++) {

            char c = s.charAt(i);

            if (c > '9' || c < '0') {
                return "";
            } else {
                num = c - '0';

                if ((len - i) % 2 == 1) {
                    num = num * 2;
                    num = num % 10 + num / 10;
                }
            }

            sum += num;
        }
        sum %= 10;
        if (sum > 0) {
            sum = 10 - sum;
        }
        return s + sum;
    }

    public static String getFixLengthNumString(String prefix, int length) {
        if (prefix == null) {
            prefix = "";
        }

        int len = prefix.length();

        if (len > length) {
            length = len;
        }

        length = length - len;

        return prefix + RandomStringUtils.randomNumeric(length);
    }

    public static void saveToClipBoard(String value) {
        if (value == null) {
            return;
        }
        StringSelection ss = new StringSelection(value);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
    }

    /**
     * 获取当前日期，参数是多少年的今天，正数就是多少年后，负数就是多少年前
     * @param years
     * @return
     */
    public static Date getDate(int years) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, years);
        Date dayTime = calendar.getTime();
        return dayTime;
    }

    public static String getDateStr(int years) {
        Date date = getDate(years);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public static String getDateStr() {
        return getDateStr(0);
    }

    public static Date getDate() {
        return getDate(0);
    }

    public static String getRandomDate(String begin, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
            Date beginDate = sdf.parse(begin);
            Date endDate = sdf.parse(end);
            long time = (Math.abs(new Random().nextLong()) + beginDate.getTime()) % endDate.getTime();
            Date selectedDate = new Date();
            selectedDate.setTime(time);
            return sdf2.format(selectedDate);
        } catch (ParseException ex) {
            Logger.getLogger(Tools.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String generateID(String areaCode, String birthday, String gender, boolean bX) {
        String sb = areaCode + birthday;

        int sex = new Random().nextInt(10);
        if ("-1".equals(gender)) {
            //不限
        } else if ("0".equals(gender)) {
            //女性
            sex = sex * 2 % 10;
        } else {
            //男性
            sex = (sex * 2 + 1) % 10;
        }

        if (bX) {
            //必须带X
            StringBuffer cardId = null;
            for (int i = 0; i < 100; i++) {
                cardId = new StringBuffer(sb);
                if (i < 10) {
                    cardId.append("0");
                }
                cardId.append(i);
                cardId.append(sex);
                String verifyCode = getVerify(cardId.toString());
                if ("x".equalsIgnoreCase(verifyCode)) {
                    return cardId.append(verifyCode).toString();
                }
            }
        } else {
            //随意
            String cardId = sb.toString();
            int i = new Random().nextInt(100);
            if (i < 10) {
                cardId = cardId + "0" + i;
            } else {
                cardId = cardId + i;
            }
            cardId = cardId + sex;
            String verifyCode = getVerify(cardId);
            return cardId + verifyCode;
        }
        return "";
    }

    private static String getVerify(String cardId) {
        String[] ValCodeArr = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
        String[] Wi = {"7", "9", "10", "5", "8", "4", "2", "1", "6", "3", "7", "9", "10", "5", "8", "4", "2"};
        int TotalmulAiWi = 0;
        for (int i = 0; i < 17; i++) {
            TotalmulAiWi = TotalmulAiWi + Integer.parseInt(String.valueOf(cardId.charAt(i))) * Integer.parseInt(Wi[i]);
        }
        int modValue = TotalmulAiWi % 11;
        String strVerifyCode = ValCodeArr[modValue];

        return strVerifyCode;
    }

    /**
     *10, 11模算法
     * @param str
     * @return
     */
    public static String getMod10_11(String str) {
        if (str == null) {
            return null;
        }

        int[] values = new int[str.length()];

        for (int i = 0; i < str.length(); i++) {
            values[i] = str.charAt(i) - '0';
        }

        int p = 10;
        int s = 0;
        for (int j = 0; j < str.length() - 1; j++) {
            s = p + values[j];
            p = s % 10 * 2;
            p = p % 11;
        }
        for (int i = 1; i < 11 - p; i++) {
            if ((i + p) % 10 == 1) {
                p = i;
                break;
            }
        }
        return str + p;
    }

    public static String getBusCode() {
        DataBaseHelper dh = new DataBaseHelper();
        List<Map<String, String>> result = dh.getDataList("select area_code from t_area_code");
        if (result == null || result.size() == 0) {
            return "";
        }
        Collections.shuffle(result);
        //前6位是区号
        StringBuffer prefix = new StringBuffer(result.get(0).get("AREA_CODE"));
        //第七位是0,1,2,3,4,5中的一个
        Random rnd = new Random();
        int type = rnd.nextInt(6);
        prefix.append(type);

        if (type < 4) {
            //内企
            //第八位是1,2中的一个
            prefix.append(rnd.nextInt(2) + 1);
            //9到10位是01,02,11,12,21,22,31,32中的一个
            prefix.append(rnd.nextInt(4));
            prefix.append(rnd.nextInt(2) + 1);
            //6位随机数字字符串
            prefix.append(RandomStringUtils.randomNumeric(6));
        } else {
            //外企
            //第八位是1~5中的一个
            prefix.append(rnd.nextInt(5) + 1);
            //第9位是0~2中的一个
            prefix.append(rnd.nextInt(3));
            //7位随机数字字符串
            prefix.append(RandomStringUtils.randomNumeric(7));
        }

        String str = prefix.toString();
        return getMod10_11(str);
    }

    public static String getOrgCode(boolean isNum) {
        int[] w = {3, 7, 9, 10, 5, 8, 4, 2};
        String prefix = null;

        if (isNum) {
            prefix = RandomStringUtils.randomNumeric(w.length);
        } else {
            prefix = RandomStringUtils.randomAlphanumeric(w.length).toUpperCase();
        }
        int sum = 0;
        for (int i = 0; i < prefix.length(); i++) {
            int code = prefix.charAt(i) - '0';
            code = code * w[i];
            sum = sum + code;
        }
        sum = sum % 11;

        if (sum == 10) {
            return prefix + "-" + "X";
        } else {
            return prefix + "-" + sum;
        }
    }

    /**
     * 字节数组转为十六进制字符串
     * @param md
     * @return
     */
    public static String convert2Hex(byte[] md) {
        if (md == null || md.length == 0) {
            return "";
        }
        int j = md.length;
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char ca[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            ca[k++] = hexDigits[byte0 >>> 4 & 0xf];
            ca[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(ca);
    }

    public static String getMD5(String str, String method) {
        try {
            byte[] btInput = str.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance(method);
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            return convert2Hex(md);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getMD5(File file, String method){
        if(file == null || !file.canRead()){
            return "";
        }

        FileInputStream fis = null;
        try {
            MessageDigest md = MessageDigest.getInstance(method);
            fis = new FileInputStream(file);
            //100KB each time
            byte[] buffer = new byte[1024*100];
            int length;
            long loopCount = 0;
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
                loopCount++;
            }
            return new String(convert2Hex(md.digest()));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getTaxCode() {
        return null;
    }

    public static void main(String[] args) {
        System.out.println(getMod10_11("53250110000630"));
    }
}
