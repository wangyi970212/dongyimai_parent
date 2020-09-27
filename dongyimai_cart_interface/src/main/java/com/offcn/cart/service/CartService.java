package com.offcn.cart.service;

import com.offcn.group.Cart;

import java.util.List;

public interface CartService {
    /**
     * 添加购物车
     * @param cartList
     * @param itemId
     * @param num
     * @return
     */

    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num );

    /**
     * 从Redis查询购物车
     */
    public List<Cart> findCartListFromRedis(String username);

    /**
     * 将购物车保存到redis
     * @param username
     * @param cartList
     */
    public void saveCartListToRedis(String username,List<Cart> cartList);

    /**
     * 合并购物车
     * @param cartList1
     * @param cartList2
     * @return
     */
    public List<Cart> mergeCartList(List<Cart> cartList1,List<Cart> cartList2);
}
