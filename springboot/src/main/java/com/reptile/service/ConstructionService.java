package com.reptile.service;

import com.reptile.util.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ConstructionService {
	@Autowired
	  private application applications;
	  private Logger logger= LoggerFactory.getLogger(ConstructionService.class);
	  public Map<String,Object> check(HttpServletRequest request,String UserCard,String UserCode,String CodePass,String UUID){
		  Map<String,Object>map=new HashMap<String,Object>();
		  System.out.println(Thread.currentThread().getName());  
			System.setProperty(ConstantInterface.chromeDriverKey, ConstantInterface.chromeDriverValue);
			WebDriver driver = new ChromeDriver();
			driver.get("http://creditcard.ccb.com/tran/WCCMainPlatV5?CCB_IBSVersion=V5&SERVLET_NAME=WCCMainPlatV5&TXCODE=NE3050");
			try {
				
				driver.switchTo().frame("itemiframe");
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				WebElement Acc_no_temp= driver.findElement(By.id("ACC_NO_temp"));
				Acc_no_temp.click();
				Acc_no_temp.sendKeys(UserCode);
				driver.findElement(By.id("LOGPASS")).sendKeys(CodePass);
				driver.findElement(By.className("btn_blue")).click();//点击登录按钮
				String detailedpage1= driver.getPageSource();
				if(detailedpage1.contains("账号或卡号与账户类型不匹配")){
					PushSocket.push(map, UUID, "0001");
					PushState.state(UserCard, "bankBillFlow",200);
					driver.close();
					map.clear();
//					logger.warn("建设账号或卡号与账户类型不匹配"+UserCode);
					map.put("errorInfo","账号或卡号与账户类型不匹配");
					map.put("errorCode","0002");
					return map;
				}
				if(detailedpage1.contains("您输入的密码不正确")){
					PushSocket.push(map, UUID, "0001");
					PushState.state(UserCard, "bankBillFlow",200);
					driver.close();
					map.clear();
					logger.warn("您输入的密码不正确"+UserCode);
					map.put("errorInfo","您输入的密码不正确");
					map.put("errorCode","0002");
					return map;
				}
				
				
				System.out.println(driver.findElement(By.className("crd_box")).getText()) ;//账单日，可用额度，信用额度，取款额度信息
				driver.switchTo().frame("result1");
				WebElement table= driver.findElement(By.className("pbd_table_form"));//获取table ,再获取其中a标签
				WebElement a= table.findElement(By.tagName("a"));
				PushState.state(UserCard, "bankBillFlow",100);
				List<String> html =new ArrayList<String>();
				PushSocket.push(map, UUID, "0000");
				for (int i = 1; i < 7; i++) {
					Map<String,Object>map1=new HashMap<String,Object>();
					driver.findElement(By.className("select_value")).click();
					driver.findElement(By.xpath("//*[@id='jqueryFrom']/div/table/tbody/tr/td[3]/div/div/ul/li["+i+"]")).click();
					a.click();
					String detailedpage2=driver.getPageSource();
					driver.switchTo().frame("result2");
					String text= driver.getPageSource();
					String detailedpage3= driver.getPageSource();
					html.add(detailedpage1+detailedpage2+detailedpage3);
					driver.switchTo().parentFrame();
					System.out.println("-----------"+i+"--------------");
				}
				Map<String,Object> con=new HashMap<String, Object>();
				Map<String,Object> data=new HashMap<String,Object>();
			  	System.out.println("页面已经放置到html中");
			  	data.put("html", html);
			  	data.put("backtype","CCNB");
			  	data.put("idcard",UserCard);
			  	con.put("data", data);
				Resttemplate resttemplate = new Resttemplate();
				map=resttemplate.SendMessage(con,applications.getSendip()+"/HSDC/BillFlow/BillFlowByreditCard");
				
				 if(map!=null&&"0000".equals(map.get("errorCode").toString())){
				    	PushState.state(UserCard, "bankBillFlow",300);
		                map.put("errorInfo","推送成功");
		                map.put("errorCode","0000");
		                driver.close();

		            }else{
		            	//--------------------数据中心推送状态----------------------
		            	logger.warn("建设推送失败"+UserCode+map);
		            	PushState.state(UserCard, "bankBillFlow",200);
		            	//---------------------数据中心推送状态----------------------
		            	driver.close();

		            }
			} catch (Exception e) { 
				// TODO Auto-generated catch block
					e.printStackTrace();
					PushState.state(UserCard, "bankBillFlow",200);
					logger.warn("建设银行账单获取失败"+UserCard);
					driver.close();
					e.printStackTrace();
					map.clear();
					map.put("errorInfo","获取账单失败");
					map.put("errorCode","0002");
			}
			
		return map;
		  
	  }
}
