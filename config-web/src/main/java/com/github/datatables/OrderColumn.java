package com.github.datatables;

import java.io.Serializable;

/**
 * 排序字段
 * Created by lirui on 15/2/8.
 */
public class OrderColumn implements Serializable {
  private int column;
  private String dir;

  public OrderColumn(int column, String dir) {
    this.column = column;
    this.dir = dir;
  }

  public int getColumn() {
    return column;
  }

  public void setColumn(int column) {
    this.column = column;
  }

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }
}
