package com.crawl.zhihu.parser;

import com.crawl.core.parser.DetailPageParser;
import com.crawl.zhihu.ZhiHuHttpClient;
import com.crawl.zhihu.entity.Page;
import com.crawl.zhihu.entity.ZhihuUserInfo;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * https://www.zhihu.com/people/wo-yan-chen-mo/following
 * 新版following页面解析出用户详细信息
 */
public class ZhiHuNewUserDetailPageParser implements DetailPageParser {
    private volatile static ZhiHuNewUserDetailPageParser instance;
    public static ZhiHuNewUserDetailPageParser getInstance(){
        if (instance == null){
            synchronized (ZhiHuHttpClient.class){
                if (instance == null){
                    instance = new ZhiHuNewUserDetailPageParser();
                }
            }
        }
        return instance;
    }
    private ZhiHuNewUserDetailPageParser(){

    }
    public ZhihuUserInfo parseDetailPage(Page page) {
        Document doc = Jsoup.parse(page.getHtml());
        ZhihuUserInfo zhihuUserInfo = new ZhihuUserInfo();
        String userToken = getUserToken(page.getUrl());
        zhihuUserInfo.setUserToken(userToken);
        zhihuUserInfo.setUrl("https://www.zhihu.com/people/" + userToken);//用户主页
        getUserByJson(zhihuUserInfo, userToken, doc.select("[data-state]").first().attr("data-state"));
        return zhihuUserInfo;
    }
    private void getUserByJson(ZhihuUserInfo zhihuUserInfo, String userToken, String dataStateJson){

        String type = "['" + userToken + "']";//转义
        String commonJsonPath = "$.entities.users." + type;
        try {
            JsonPath.parse(dataStateJson).read(commonJsonPath);
        } catch (PathNotFoundException e){
            commonJsonPath = "$.entities.users.null";
        }
        setUserInfoByJsonPth(zhihuUserInfo, "username", dataStateJson, commonJsonPath + ".name");//username
        setUserInfoByJsonPth(zhihuUserInfo, "hashId", dataStateJson, commonJsonPath + ".id");//hashId
        setUserInfoByJsonPth(zhihuUserInfo, "followees", dataStateJson, commonJsonPath + ".followingCount");//关注人数
        setUserInfoByJsonPth(zhihuUserInfo, "location", dataStateJson, commonJsonPath + ".locations[0].name");//位置
        setUserInfoByJsonPth(zhihuUserInfo, "business", dataStateJson, commonJsonPath + ".business.name");//行业
        setUserInfoByJsonPth(zhihuUserInfo, "employment", dataStateJson, commonJsonPath + ".employments[0].company.name");//公司
        setUserInfoByJsonPth(zhihuUserInfo, "position", dataStateJson, commonJsonPath + ".employments[0].job.name");//职位
        setUserInfoByJsonPth(zhihuUserInfo, "education", dataStateJson, commonJsonPath + ".educations[0].school.name");//学校
        setUserInfoByJsonPth(zhihuUserInfo, "answers", dataStateJson, commonJsonPath + ".answerCount");//回答数
        setUserInfoByJsonPth(zhihuUserInfo, "asks", dataStateJson, commonJsonPath + ".questionCount");//提问数
        setUserInfoByJsonPth(zhihuUserInfo, "posts", dataStateJson, commonJsonPath + ".articlesCount");//文章数
        setUserInfoByJsonPth(zhihuUserInfo, "followers", dataStateJson, commonJsonPath + ".followerCount");//粉丝数
        setUserInfoByJsonPth(zhihuUserInfo, "agrees", dataStateJson, commonJsonPath + ".voteupCount");//赞同数
        setUserInfoByJsonPth(zhihuUserInfo, "thanks", dataStateJson, commonJsonPath + ".thankedCount");//感谢数
        try {
            Integer gender = JsonPath.parse(dataStateJson).read(commonJsonPath + ".gender");
            if (gender != null && gender == 1){
                zhihuUserInfo.setSex("male");
            }
            else if(gender != null && gender == 0){
                zhihuUserInfo.setSex("female");
            }
        } catch (PathNotFoundException e){
            //没有该属性
        }

    }

    /**
     * jsonPath获取值，并通过反射直接注入到user中
     * @param zhihuUserInfo
     * @param fieldName
     * @param json
     * @param jsonPath
     */
    private void setUserInfoByJsonPth(ZhihuUserInfo zhihuUserInfo, String fieldName, String json, String jsonPath){
        try {
            Object o = JsonPath.parse(json).read(jsonPath);
            Field field = zhihuUserInfo.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(zhihuUserInfo, o);
        } catch (PathNotFoundException e1) {
            //no results
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 根据url解析出用户id
     * @param url
     * @return
     */
    private String getUserToken(String url){
        Pattern pattern = Pattern.compile("https://www.zhihu.com/[a-z]+/(.*)/following");
        Matcher matcher = pattern.matcher(url);
        String userId = null;
        if(matcher.find()){
            userId = matcher.group(1);
            return userId;
        }
        throw new RuntimeException("not parseListPage userId");
    }
}