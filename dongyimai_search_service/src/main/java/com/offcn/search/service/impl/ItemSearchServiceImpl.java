package com.offcn.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(timeout = 30000)
public class ItemSearchServiceImpl implements ItemSearchService {
    @Autowired
    private SolrTemplate solrTemplate;
    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public Map<String, Object> search(Map searchMap) {
        //用来存储查询条件
        Map<String,Object> map=new HashMap<String, Object>();
       //查询列表
        map.putAll(searchList(searchMap));

        //根据关键字查询商品分类
        List categoryList = searchCategoryList(searchMap);
        map.put("categoryList",categoryList);
        //3、根据商品类目查询对应的品牌、规格
        //读取分类名称
        String categoryName=(String) searchMap.get("category");
        if(!"".equals(categoryName)) {
            //按照分类名称重新读取对应品牌、规格
            map.putAll(searchBrandAndSpecList(categoryName));
        }else {
            if (categoryList.size() > 0) {
                Map mapBrandAndSpec = searchBrandAndSpecList((String) categoryList.get(0));
                map.putAll(mapBrandAndSpec);
            }
        }
        return  map;

    }

    @Override
    public void importList(List<TbItem> list) {
        for (TbItem tbItem : list) {
            System.out.println(tbItem.getTitle());
            Map<String,String> specMap = JSON.parseObject(tbItem.getSpec(), Map.class);
            Map map = new HashMap();
            for (String key : specMap.keySet()) {
                map.put("item_spec_"+Pinyin.toPinyin(key,"").toLowerCase(),specMap.get(key));

            }
            tbItem.setSpecMap(map);

        }
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }

    @Override
    public void deleteByGoodsIds(Long[] goodsIds) {
        System.out.println("删除索引库相关信息");
        SimpleQuery query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsid").in(goodsIds);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();

    }

    //根据关键字查询高亮显示
    private Map searchList(Map searchMap){
        Map map=new HashMap();
        //创建一个可以支持高亮显示的查询对象
        SimpleHighlightQuery query = new SimpleHighlightQuery();
        //设定需要高亮处理的字段
        HighlightOptions highlightOptions = new HighlightOptions();
        highlightOptions.addField("item_title");
        //设置高亮的前缀和后缀
        highlightOptions.setSimplePrefix("<em style='color:red'>");
        highlightOptions.setSimplePostfix("</em>");
        //关联高亮选项到高亮查询对象中
        query.setHighlightOptions(highlightOptions);
        //设置查询对象，根据关键字查询

        //处理关键字
        if(searchMap.get("keywords")!=null){
            int index=searchMap.get("keywords").toString().indexOf(" ");
            if(index>0){
                searchMap.put("keywords",searchMap.get("keywords").toString().replaceAll(" ",""));
            }
        }

           //创建查询对象
        //按照关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        //按照分类查询
        if(!"".equals(searchMap.get("category")) ){
            Criteria criteria1 = new Criteria("item_category").is(searchMap.get("category"));
            FilterQuery filterQuery = new SimpleFilterQuery(criteria1);
            query.addFilterQuery(filterQuery);
        }
        //根据品牌查询
        if(!"".equals(searchMap.get("brand")) ){
            Criteria criteria2 = new Criteria("item_brand").is(searchMap.get("brand"));
            FilterQuery filterQuery2 = new SimpleFilterQuery(criteria2);
            query.addFilterQuery(filterQuery2);
        }
        //根据规格查询
        if(searchMap.get("spec")!=null){
            Map<String,String> specMap=(Map)searchMap.get("spec");
            for (String key : specMap.keySet()) {
                Criteria criteria3 = new Criteria("item_spec_"+ Pinyin.toPinyin(key,"").toLowerCase()).is(specMap.get(key));
                FilterQuery filterQuery3 = new SimpleFilterQuery(criteria3);
                query.addFilterQuery(filterQuery3);
            }
        }
        //根据价格查询
        if(!"".equals(searchMap.get("price"))){
            String[] price = ((String) searchMap.get("price")).split("-");
            if(!price[0].equals("0")){//如果区间起点不等于0
                Criteria filterCriteria=new Criteria("item_price").greaterThanEqual(price[0]);
                FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            if(!price[1].equals("*")){//如果区间终点不等于*
                Criteria filterCriteria=new  Criteria("item_price").lessThanEqual(price[1]);
                FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }



        //根据分页查询
        Integer pageNo=(Integer)searchMap.get("pageNo");
        if(pageNo==null){
            pageNo=1;//默认第一页
        }
        Integer pageSize=(Integer)searchMap.get("pageSize");
        if(pageSize==null){
            pageSize=10;//默认显示10条
        }
        query.setOffset((pageNo-1)*pageSize);//查询起始页
        query.setRows(pageSize);//查询多少条记录

        //按照价格排序输出
        String sortValue=(String)searchMap.get("sort");//asc desc
        String sortField=(String)searchMap.get("sortField");
        //判断排序是升序还是降序；
        if(sortValue!=null&& !sortValue.equals("")){
            if(sortValue.equals("ASC")){
                Sort sort= new Sort(Sort.Direction.ASC,"item_"+sortField);
                query.addSort(sort);
            }
            if(sortValue.equals("DESC")){
                Sort sort1= new Sort(Sort.Direction.DESC,"item_"+sortField);
                query.addSort(sort1);
            }
        }



           //关联查询对象到高亮查询器对象
        query.addCriteria(criteria);
        //发出带高亮数据查询请求
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //获取高亮集合入口
        List<HighlightEntry<TbItem>> highlightEntryList = page.getHighlighted();
        //遍历高亮集合
        for (HighlightEntry<TbItem> highlightEntry : highlightEntryList) {
            TbItem tbItem = highlightEntry.getEntity();
            if(highlightEntry.getHighlights().size()>0&&highlightEntry.getHighlights().get(0).getSnipplets().size()>0){
                List<HighlightEntry.Highlight> highlightList = highlightEntry.getHighlights();
                List<String> snipplets = highlightList.get(0).getSnipplets();
                tbItem.setTitle(snipplets.get(0));
            }

        }

        map.put("rows",page.getContent());
        map.put("totalPages",page.getTotalPages());
        map.put("total",page.getTotalElements());
        return map;
    }

    //查询分类的列表
    private  List searchCategoryList(Map searchMap){
        List<String> list=new ArrayList<String>();
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        //将查询条件对象添加到查询器对象上
        query.addCriteria(criteria);
        //设置分组选项
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        //将分组选项添加到查询对象上
        query.setGroupOptions(groupOptions);
        //查询索引库得到分组页面
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //根据列得到分组的结果集
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //得到分组结果入口页
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        List<GroupEntry<TbItem>> groupEntryList = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : groupEntryList) {
            list.add(entry.getGroupValue());
        }
        return  list;


    }

    //查询品牌和规格列表
    private Map searchBrandAndSpecList(String category){
        Map map=new HashMap();
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);//获取模板ID
        if(typeId!=null){
            //根据模板id查询品牌列表
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
            map.put("brandList", brandList);
            //根据模板id查询规格列表
            List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
            map.put("specList", specList);
        }
        return map;
    }


}
