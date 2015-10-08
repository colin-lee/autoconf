package com.github.datatables;

import java.io.Serializable;
import java.util.List;

/**
 * 服务端响应
 * Created by lirui on 2014/11/2.
 */
public class DataResponse<T> implements Serializable {
  /**
   * 把请求对应的draw参数回填到响应中
   */
  private String draw;
  /**
   * 所有记录条数
   */
  private int recordsTotal;
  /**
   * 过滤后的记录条数
   */
  private int recordsFiltered;
  /**
   * 错误提示消息
   */
  private String error;
  /**
   * 当前页的记录
   */
  private List<T> data;

  public String getDraw() {
    return draw;
  }

  public void setDraw(String draw) {
    this.draw = draw;
  }

  public int getRecordsTotal() {
    return recordsTotal;
  }

  public void setRecordsTotal(int recordsTotal) {
    this.recordsTotal = recordsTotal;
  }

  public int getRecordsFiltered() {
    return recordsFiltered;
  }

  public void setRecordsFiltered(int recordsFiltered) {
    this.recordsFiltered = recordsFiltered;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }
}
