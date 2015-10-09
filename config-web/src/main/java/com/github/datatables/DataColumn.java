package com.github.datatables;

import java.io.Serializable;

/**
 * 单个数据列
 *
 * Created by lirui on 15/2/8.
 */
public class DataColumn implements Serializable {
  private String data;
  private String name;
  private boolean searchable;
  private boolean orderable;
  private SearchColumn search;

  public DataColumn(String data, String name, boolean searchable, boolean orderable) {
    this.data = data;
    this.name = name;
    this.searchable = searchable;
    this.orderable = orderable;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public boolean isOrderable() {
    return orderable;
  }

  public void setOrderable(boolean orderable) {
    this.orderable = orderable;
  }

  public SearchColumn getSearch() {
    return search;
  }

  public void setSearch(SearchColumn search) {
    this.search = search;
  }
}
