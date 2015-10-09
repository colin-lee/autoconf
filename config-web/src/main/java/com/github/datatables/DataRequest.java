package com.github.datatables;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;

/**
 * datatable新版请求
 * Created by lirui on 2015/2/8.
 */
public class DataRequest implements Serializable {
  private String draw;
  private int start;
  private int length;
  private SearchColumn search;
  private List<DataColumn> columns;
  private List<OrderColumn> orders;

  /**
   * 解析请求参数
   *
   * <pre>
   * draw:1
   * columns[0][data]:name
   * columns[0][name]:
   * columns[0][searchable]:true
   * columns[0][orderable]:true
   * columns[0][search][value]:
   * columns[0][search][regex]:false
   * order[0][column]:0
   * order[0][dir]:asc
   * start:0
   * length:50
   * search[value]: xyz
   * search[regex]:false
   * </pre>
   *
   * @param p
   * @return
   */
  public static DataRequest parse(HttpServletRequest p) {
    DataRequest req = new DataRequest();
    req.setDraw(p.getParameter("draw"));
    req.setStart(Integer.parseInt(p.getParameter("start")));
    req.setLength(Integer.parseInt(p.getParameter("length")));

    // 解析全局搜索字段
    String s = p.getParameter("search[value]");
    if (!Strings.isNullOrEmpty(s)) {
      boolean b = Boolean.parseBoolean(p.getParameter("search[regex]"));
      req.setSearch(new SearchColumn(s, b));
    }

    // 解析排序字段
    List<OrderColumn> orders = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      String name = String.format("order[%d][column]", i);
      s = p.getParameter(name);
      if (Strings.isNullOrEmpty(s))
        break;
      name = String.format("order[%d][dir]", i);
      String dir = p.getParameter(name);
      int column = Integer.parseInt(s);
      orders.add(new OrderColumn(column, dir));
    }
    req.setOrders(orders);

    // 解析数据列
    List<DataColumn> columns = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      String key = String.format("columns[%d][data]", i);
      String data = p.getParameter(key);
      if (Strings.isNullOrEmpty(data))
        break;
      key = String.format("columns[%d][name]", i);
      String name = p.getParameter(key);
      key = String.format("columns[%d][searchable]", i);
      boolean searchable = Boolean.parseBoolean(p.getParameter(key));
      key = String.format("columns[%d][orderable]", i);
      boolean orderable = Boolean.parseBoolean(p.getParameter(key));
      DataColumn col = new DataColumn(data, name, searchable, orderable);
      columns.add(col);
      key = String.format("columns[%d][search][value]", i);
      s = p.getParameter(key);
      if (!Strings.isNullOrEmpty(s)) {
        key = String.format("columns[%d][search][regex]", i);
        boolean b = Boolean.parseBoolean(p.getParameter(key));
        col.setSearch(new SearchColumn(s, b));
      }
    }
    req.setColumns(columns);

    return req;
  }

  public String getDraw() {
    return draw;
  }

  public void setDraw(String draw) {
    this.draw = draw;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public int getStop() {
    return start + length;
  }

  public SearchColumn getSearch() {
    return search;
  }

  public void setSearch(SearchColumn search) {
    this.search = search;
  }

  public List<DataColumn> getColumns() {
    return columns;
  }

  public void setColumns(List<DataColumn> columns) {
    this.columns = columns;
  }

  public List<OrderColumn> getOrders() {
    return orders;
  }

  public void setOrders(List<OrderColumn> orders) {
    this.orders = orders;
  }
}
