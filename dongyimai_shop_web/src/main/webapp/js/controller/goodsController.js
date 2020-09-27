 //控制层 
app.controller('goodsController' ,function($scope,$controller ,$location,goodsService,uploadService,itemCatService,typeTemplateService){
	
	$controller('baseController',{$scope:$scope});//继承
	
    //读取列表数据绑定到表单中  
	$scope.findAll=function(){
		goodsService.findAll().success(
			function(response){
				$scope.list=response;
			}			
		);
	}    
	
	//分页
	$scope.findPage=function(page,rows){			
		goodsService.findPage(page,rows).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	
	//查询实体 
	$scope.findOne=function(id){
		var id=$location.search()['id'];//获取请求的id
		if(id==null){
			return ;
		}

		goodsService.findOne(id).success(
			function(response){
				$scope.entity= response;
				//添加到富文本编辑器中
				editor.html($scope.entity.goodsDesc.introduction);
				//将图片数据取出
				$scope.entity.goodsDesc.itemImages=JSON.parse($scope.entity.goodsDesc.itemImages);
				//还原扩展属性
				$scope.entity.goodsDesc.customAttributeItems=JSON.parse($scope.entity.goodsDesc.customAttributeItems);
				//还原规格
				$scope.entity.goodsDesc.specificationItems=JSON.parse($scope.entity.goodsDesc.specificationItems);
				//还原sku
				for(var i=0;i<$scope.entity.itemList.length;i++){
					$scope.entity.itemList[i].spec=JSON.parse($scope.entity.itemList[i].spec);
				}
			}
		);				
	}
	
	//保存 
	$scope.save=function(){
		//提取文本编辑器的值
		$scope.entity.goodsDesc.introduction=editor.html();
		var serviceObject;//服务层对象  				
		if($scope.entity.goods.id!=null){//如果有ID
			serviceObject=goodsService.update( $scope.entity ); //修改  
		}else{
			serviceObject=goodsService.add( $scope.entity  );//增加 
		}				
		serviceObject.success(
			function(response){
				if(response.success){
					//重新查询 
					/*alert('保存成功');
					$scope.entity={};
					editor.html("");*/
					location.href="goods.html";
				}else{
					alert(response.message);
				}
			}		
		);				
	}
	
	 
	//批量删除 
	$scope.dele=function(){			
		//获取选中的复选框			
		goodsService.dele( $scope.selectIds ).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新列表
					$scope.selectIds=[];
				}						
			}		
		);				
	}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		goodsService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}

	//添加商品信息
	$scope.add=function () {
		//从富文本编辑器中取值
		$scope.entity.goodsDesc.introduction=editor.html();

		goodsService.add($scope.entity).success(function (response) {
			if(response.success){
				//重新查询
				alert("保存成功")
				$scope.entity={goodsDesc:{itemImages:[],specificationItems:[]}};//清空
				editor.html('');//清空富文本
			}else{
				alert(response.message);
			}
		})
	}

	//上传图片
	$scope.uploadFile=function(){
		uploadService.uploadFile().success(function(response) {
			if(response.success){//如果上传成功，取出url
				$scope.image_entity.url=response.message;//设置文件地址
			}else{
				alert(response.message);
			}
		}).error(function() {
			alert("上传发生错误");
		});
	};

	$scope.entity={goods:{},goodsDesc:{itemImages:[]}};//定义页面实体结构
	//添加图片列表
	$scope.add_image_entity=function(){
		$scope.entity.goodsDesc.itemImages.push($scope.image_entity);
	}

	//列表中移除图片
	$scope.remove_image_entity=function(index){
		$scope.entity.goodsDesc.itemImages.splice(index,1);
	}

	//查询一级分类
	$scope.selectItemCatList=function () {
		itemCatService.findByParentId(0).success(function (response) {
			$scope.itemCat1List=response;
		})
	}
	//查询二级分类
	$scope.$watch('entity.goods.category1Id',function (newValue,oldValue) {

			if(newValue){
				itemCatService.findByParentId(newValue).success(function (response) {
				$scope.itemCat2List=response;
				})
			}

	})
	//查询三级分类
	$scope.$watch('entity.goods.category2Id',function (newValue,oldValue) {

		if(newValue){
			itemCatService.findByParentId(newValue).success(function (response) {
				$scope.itemCat3List=response;
			})
		}

	})

	//获取模板类型的id
	$scope.$watch('entity.goods.category3Id',function (newValue,oldValue) {

		if(newValue){
			itemCatService.findOne(newValue).success(function (response) {
				$scope.entity.goods.typeTemplateId=response.typeId;
			})
		}
	})

	//根据模板id获取品牌信息和扩展属性
	$scope.$watch('entity.goods.typeTemplateId',function (newValue,oldValue) {
		if(newValue){
			typeTemplateService.findOne(newValue).success(function (response) {
				$scope.typeTemplate=response;
				$scope.typeTemplate.brandIds=JSON.parse($scope.typeTemplate.brandIds);
				//如果页面内没有传递商品id，就添加，否则修改
				if($location.search()['id']==null){
					$scope.entity.goodsDesc.customAttributeItems=JSON.parse($scope.typeTemplate.customAttributeItems);
				}
			})
			//查询规格列表
			typeTemplateService.findSpecList(newValue).success(function (response) {
				$scope.specList=response;
			})
		}

	})

	$scope.entity={ goodsDesc:{itemImages:[],specificationItems:[]}  };

	$scope.updateSpecAttribute=function($event,name,value){
		var object= $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems ,'attributeName', name);
		if(object!=null){
			if($event.target.checked ){
				object.attributeValue.push(value);
			}else{
				//取消勾选
				object.attributeValue.splice( object.attributeValue.indexOf(value ) ,1);//移除选项
				//如果选项都取消了，将此条记录移除
				if(object.attributeValue.length==0){
					$scope.entity.goodsDesc.specificationItems.splice($scope.entity.goodsDesc.specificationItems.indexOf(object),1);
				}
			}
		}else{
			$scope.entity.goodsDesc.specificationItems.push({"attributeName":name,"attributeValue":[value]});
		}
	}

	//创建SKU列表
	$scope.createItemList=function () {
		//初始一个不带规格的
		$scope.entity.itemList=[{spec:{},price:0,num:9999,status:'0',isDefault:'0'}];
		var items=$scope.entity.goodsDesc.specificationItems;
		for(var i=0;i<items.length;i++){
			$scope.entity.itemList=addColumn($scope.entity.itemList,items[i].attributeName,items[i].attributeValue);

		}
	}

	//添加列值
	addColumn=function (list,columnName,columnValues) {
		var newList=[];
		for(var i=0;i<list.length;i++){
			var oldRow=list[i];
			for(var j=0;j<columnValues.length;j++){
				//深层克隆
				var newRow=JSON.parse(JSON.stringify(oldRow));
				newRow.spec[columnName]=columnValues[j];
				newList.push(newRow);
			}
		}
		return newList;
	}

	//设定商品状态
	$scope.status=['未审核','已审核','审核未通过','关闭'];

	$scope.itemCatList=[];
	//设置获取所有分类
	$scope.findItemCatList=function () {
		itemCatService.findAll().success(function (response) {
			for(var i=0;i<response.length;i++){
				$scope.itemCatList[response[i].id]=response[i].name;
			}
		})
	}

	//根据规格名称回显数据
	$scope.checkAttributeValue=function (specName,optionName) {
		var items=$scope.entity.goodsDesc.specificationItems;
		var object=$scope.searchObjectByKey(items,'attributeName',specName);
		if(object==null){
			return false;
		}else {
			if(object.attributeValue.indexOf(optionName)>=0){
				return typeTemplateService
			}else {
				return false;
			}
		}
	}


});	