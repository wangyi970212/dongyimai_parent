app.service("searchService",function ($http) {
//根据条件查询商品sku信息
    this.search=function (searchMap) {
        return $http.post('itemsearch/search.do',searchMap);
    }
});