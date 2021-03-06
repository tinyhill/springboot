package com.reptile.service.depositCard;

import com.reptile.util.ConstantInterface;
import com.reptile.util.PushSocket;
import com.reptile.util.Resttemplate;
import com.reptile.util.RobotUntil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class ChinaBankDepositCardService {
    private Logger logger = LoggerFactory.getLogger(ChinaBankDepositCardService.class);

    /**
     * 中国银行储蓄卡账单获取
     * @param request
     * @param IDNumber
     * @param cardNumber
     * @param passWord
     * @param userName
     * @return
     */
    public Map<String, Object> getDetailMes(HttpServletRequest request, String IDNumber, String cardNumber, String passWord, String userName,String UUID) {
        Map<String, Object> map = new HashMap<>();
        List<String> dataList = new ArrayList<>();
        String path = request.getServletContext().getRealPath("/vecImageCode");
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        logger.warn("中国银行储蓄卡登录...");
        System.setProperty(ConstantInterface.chromeDriverKey, ConstantInterface.chromeDriverValue);
        System.out.println("~~");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        ChromeDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, 10);
        try {
            driver.get("https://ebsnew.boc.cn/boc15/login.html");
            List<WebElement> input = driver.findElementsByTagName("input");
            //输入卡号
            input.get(0).sendKeys(cardNumber);
            Thread.sleep(1000);
            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.TAB).build().perform();
            Thread.sleep(3000);
            //判断是否为有效卡号
            String msgContent = driver.findElement(By.id("msgContent")).getText();
            if (msgContent.length() != 0) {
                map.put("errorCode", "0001");
                map.put("errorInfo", msgContent);
                driver.quit();
                return map;
            }
            WebElement imageCode;
            try {
                imageCode = driver.findElement(By.id("captcha_debitCard"));
            } catch (Exception e) {
                map.put("errorCode", "0002");
                map.put("errorInfo", "请输入正确的储蓄卡号");
                driver.quit();
                return map;
            }
            //输入密码
            input = driver.findElementsByTagName("input");
            input.get(4).sendKeys(passWord);
            Thread.sleep(1000);

            //识别验证码
            String code = new RobotUntil().getImgFileByScreenshot(imageCode, driver, file);
            //输入验证码
            input.get(5).sendKeys(code.toLowerCase());
            //提交信息登录
            List<WebElement> elements = driver.findElements(By.className("btn"));
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).getText().contains("查询")) {
                    elements.get(i).click();
                    break;
                }
            }
            Thread.sleep(5000);
            //判断登录是否成功
            msgContent = driver.findElement(By.id("msgContent")).getText();
            if (msgContent.length() != 0) {
                if (msgContent.contains("验证码输入错误")) {
                    map.put("errorCode", "0003");
                    map.put("errorInfo", "当前系统繁忙，请刷新页面重新认证！");
                } else {
                    map.put("errorCode", "0004");
                    map.put("errorInfo", msgContent);
                }
                driver.quit();
                return map;
            }
            logger.warn("中国银行储蓄卡登录成功");
            //--------------这里加推送状态
            PushSocket.push(map, UUID, "0000");
            //获取储蓄卡基本信息
            WebElement cardMain = driver.findElementById("cardMain");
            map.put("baseMes", cardMain.getAttribute("innerHTML"));
            logger.warn("中国银行储蓄卡基本信息获取成功");
            //获取详单信息
            dataList = getItemMes(dataList, driver);
            logger.warn("中国银行储蓄卡信息解析成功");
            map.put("itemMes", dataList);
            //解析获得的数据
            map = analyData(map);

            map.put("IDNumber", IDNumber);
            map.put("cardNumber", cardNumber);
            map.put("userName", userName);
            map.put("bankName", "中国银行");
            //推送数据
            map = new Resttemplate().SendMessage(map, ConstantInterface.port+"/HSDC/savings/authentication");
            logger.warn("中国银行储蓄卡账单信息推送完成");
            driver.quit();
        } catch (Exception e) {
            map.clear();
            logger.warn("认证中国储蓄卡出错", e);
            driver.quit();
            map.put("errorCode", "0003");
            map.put("errorInfo", "系统异常");
        }
        return map;
    }

    /**
     *
     * 获取账单详情信息
     * @param dataList
     * @param driver
     * @return
     * @throws InterruptedException
     */

    public  List<String> getItemMes(List<String> dataList, ChromeDriver driver) throws Exception {
        //切换至交易明细
        List<WebElement> element = driver.findElements(By.className("tabs"));
        for (int i = 0; i < element.size(); i++) {
            if (element.get(i).getText().contains("交易明细")) {
                element.get(i).click();
            }
        }
        WebElement debitCardTransDetail_table = null;
        Thread.sleep(2000);
        SimpleDateFormat sim = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        String endTime = sim.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String beginTime = sim.format(cal.getTime());
        //循环获取6个月的账单信息
        for (int i = 0; i < 6; i++) {
            //设置查询开始时间
            driver.findElementsByClassName("input").get(0).clear();
            driver.findElementsByClassName("input").get(0).sendKeys(beginTime);
            //设置查询结束时间
            driver.findElementsByClassName("input").get(1).clear();
            driver.findElementsByClassName("input").get(1).sendKeys(endTime);
            driver.findElementsByClassName("ml10").get(1).click();
            List<WebElement> btn = driver.findElements(By.className("btn-r"));
            for (int j = 0; j < btn.size(); j++) {
                if (btn.get(j).getText().equals("查询")) {
                    btn.get(j).click();
                }
            }
            Thread.sleep(2000);
            String msgContent1 = driver.findElementById("msgContent").getText();
            //判断当月是否有账单信息
            if (msgContent1.length() != 0) {
                driver.findElementsByClassName("btn-r").get(4).click();
            } else {
                debitCardTransDetail_table = driver.findElementById("debitCardTransDetail_table");
                dataList.add(debitCardTransDetail_table.getAttribute("innerHTML"));
            }
            //上月末
            cal.add(Calendar.DAY_OF_MONTH, -1);
            endTime = sim.format(cal.getTime());
            //上月初
            cal.set(Calendar.DAY_OF_MONTH, 1);
            beginTime = sim.format(cal.getTime());
        }
        return dataList;
    }


    /**
     * 解析从页面获取到的数据并封装
     *
     * @param
     * @return
     */
    private Map<String, Object> analyData(Map<String, Object> paramMap) throws Exception {
        Map<String, Object> map = new HashMap<>();
        //账单信息
        List<String> itemMes = (List<String>) paramMap.get("itemMes");
        //基本信息
        String baseMes = paramMap.get("baseMes").toString();

        if (itemMes.size() == 0) {
            map.put("errorCode", "1001");
            map.put("errorInfo", "账单信息为空");
            return map;
        }
        List billList;
        Map<String, Object> baseMap;
        try {
            //解析账单信息
            billList = analyBillMethod(itemMes);
            //解析基本信息
            baseMap = analyBaseMes(baseMes);
        } catch (Exception e) {
            logger.warn("数据解析失败", e);
            throw new Exception("数据解析失败");
        }
        map.put("baseMes", baseMap);
        map.put("billMes", billList);
        return map;
    }

    /**
     * 解析账单信息
     *
     * @param itemMes
     * @return
     */
    private List analyBillMethod(List<String> itemMes) throws Exception {
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> detailMap;  //存放当月的交易详情

        for (int index = 0; index < itemMes.size(); index++) {
            Document parse = Jsoup.parse(itemMes.get(index));
            Elements tbody = parse.getElementsByTag("tbody");
            if (tbody == null || tbody.size() == 0) {
                break;
            }
            Elements tr = tbody.get(0).getElementsByTag("tr");
            for (int i = 0; i < tr.size(); i++) {
                Element element = tr.get(i);
                Elements td = element.getElementsByTag("td");
                detailMap = new HashMap<>();

                detailMap.put("dealTime", td.get(0).text());
                detailMap.put("dealReferral", td.get(1).text());
                detailMap.put("oppositeSideName", td.get(2).text());
                detailMap.put("oppositeSideNumber", td.get(3).text());
                detailMap.put("currency", td.get(4).text());
                detailMap.put("incomeMoney", td.get(6).text());
                detailMap.put("expendMoney", td.get(7).text());
                detailMap.put("balanceAmount", td.get(8).text());
                detailMap.put("dealDitch", td.get(9).text());
                dataList.add(detailMap);
            }
        }
        return dataList;
    }

    /**
     * 解析基本信息
     *
     * @param baseMes
     * @return
     */
    private Map<String, Object> analyBaseMes(String baseMes) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Document parse = Jsoup.parse(baseMes);
        Elements elementsByClass = parse.getElementsByClass("layout-lr");
        Elements li = elementsByClass.get(0).getElementsByTag("li");
        map.put("accountType", li.get(0).getElementsByClass("item-con").text());
        map.put("openBranch", li.get(1).getElementsByClass("item-con").text());
        map.put("openTime", li.get(2).getElementsByClass("item-con").text());
        return map;
    }

}
