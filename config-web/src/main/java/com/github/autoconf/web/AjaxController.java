package com.github.autoconf.web;

import com.github.autoconf.entity.Config;
import com.github.autoconf.entity.ConfigHistory;
import com.github.autoconf.service.ConfigHistoryService;
import com.github.autoconf.service.ConfigService;
import com.github.datatables.*;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import name.fraser.neil.plaintext.diff_match_patch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * ajax请求处理器
 * Created by lirui on 15/2/8.
 */
@RestController
public class AjaxController {
  @Autowired
  private ConfigService configService;
  @Autowired
  private ConfigHistoryService historyService;

  @PostConstruct
  void fillCache() {
    configService.findAll();
  }

  @RequestMapping(value = "/ajax/diff", method = RequestMethod.POST)
  @ResponseBody
  public String historyDiff(@RequestParam long id) {
    ConfigHistory h = historyService.findbyId(id);
    Config c = configService.findById(h.getConfigId());
    diff_match_patch dmp = new diff_match_patch();
    LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(c.getContent(), h.getContent(), true);
    dmp.diff_cleanupSemantic(diffs);
    return dmp.diff_prettyHtml(diffs);
  }

  @RequestMapping("/ajax/config/")
  @SuppressWarnings("unchecked")
  public DataResponse<Config> config(HttpServletRequest request) {
    final DataRequest req = DataRequest.parse(request);
    DataResponse<Config> res = new DataResponse<>();
    res.setDraw(req.getDraw());
    List<Config> all = configService.findAll();
    res.setRecordsTotal(all.size());
    Set<Config> filtered;
    // 全局搜索过滤
    Set<String> words = Sets.newHashSet();
    String searchWord;
    final List<DataColumn> columns = req.getColumns();
    final SearchColumn globalSearch = req.getSearch();
    if (globalSearch != null && globalSearch.isSearchable()) {
      searchWord = globalSearch.getValue().toLowerCase();
      words.add(searchWord);
      filtered = searchGlobal(all, columns, globalSearch);
    } else {
      filtered = Sets.newHashSet(all);
    }

    // 按列搜索
    List<DataColumn> searchColumns = getSearchColumns(words, columns);
    searchColumn(filtered, searchColumns);

    // 排序
    List<Config> sorted = Lists.newArrayList(filtered);
    sort(req, columns, sorted);

    // 分页
    res.setRecordsFiltered(sorted.size());
    int toIndex = Math.min(sorted.size(), req.getStop());
    List<Config> data = sorted.subList(req.getStart(), toIndex);
    highlight(data, words);
    res.setData(data);
    return res;
  }

  @RequestMapping("/ajax/history")
  @SuppressWarnings("unchecked")
  public DataResponse<ConfigHistory> history(@RequestParam(defaultValue = "0") long configId, @RequestParam(required = false) String editor, HttpServletRequest request) {
    final DataRequest req = DataRequest.parse(request);
    DataResponse<ConfigHistory> res = new DataResponse<>();
    res.setDraw(req.getDraw());
    List<ConfigHistory> all;
    if (configId > 0) {
      if (Strings.isNullOrEmpty(editor)) {
        all = historyService.findByConfigId(configId);
      } else {
        all = historyService.findByConfigIdAndEditor(configId, editor);
      }
    } else if (!Strings.isNullOrEmpty(editor)) {
      all = historyService.findByEditor(editor);
    } else {
      all = historyService.findRecent();
    }

    res.setRecordsTotal(all.size());
    Set<ConfigHistory> filtered;
    // 全局搜索过滤
    Set<String> words = Sets.newHashSet();
    String searchWord;
    final List<DataColumn> columns = req.getColumns();
    final SearchColumn globalSearch = req.getSearch();
    if (globalSearch != null && globalSearch.isSearchable()) {
      searchWord = globalSearch.getValue().toLowerCase();
      words.add(searchWord);
      filtered = searchGlobal(all, columns, globalSearch);
    } else {
      filtered = Sets.newHashSet(all);
    }

    // 按列搜索
    List<DataColumn> searchColumns = getSearchColumns(words, columns);
    searchColumn(filtered, searchColumns);

    // 排序
    List<ConfigHistory> sorted = Lists.newArrayList(filtered);
    sort(req, columns, sorted);

    // 分页
    res.setRecordsFiltered(sorted.size());
    int toIndex = Math.min(sorted.size(), req.getStop());
    List<ConfigHistory> data = sorted.subList(req.getStart(), toIndex);
    highlight(data, words);
    res.setData(data);
    return res;
  }

  @SuppressWarnings("unchecked")
  private Set searchGlobal(List<? extends Config> all, List<DataColumn> columns, SearchColumn globalSearch) {
    Set filtered = new HashSet();
    for (Config i : all) {
      for (DataColumn col : columns) {
        if (!col.isSearchable())
          continue;
        String data = col.getData();
        if (found(i, data, globalSearch)) {
          filtered.add(i);
          break;
        }
      }
    }
    return filtered;
  }

  private List<DataColumn> getSearchColumns(Set<String> words, List<DataColumn> columns) {
    List<DataColumn> searchColumns = Lists.newArrayList();
    for (DataColumn i : columns) {
      if (i.isSearchable() && i.getSearch() != null && i.getSearch().isSearchable()) {
        searchColumns.add(i);
        if (i.getData().equals("summary")) {
          words.add(i.getSearch().getValue());
        }
      }
    }
    return searchColumns;
  }

  private void searchColumn(Set<? extends Config> filtered, List<DataColumn> searchColumns) {
    if (searchColumns.size() > 0) {
      final Iterator<? extends Config> it = filtered.iterator();
      while (it.hasNext()) {
        Config c = it.next();
        // 所有列内容必须都满足
        boolean hit = true;
        for (DataColumn i : searchColumns) {
          if (!found(c, i.getData(), i.getSearch())) {
            hit = false;
            break;
          }
        }
        if (!hit)
          it.remove();
      }
    }
  }

  /**
   * 按照某个字段排序
   */
  private void sort(DataRequest req, final List<DataColumn> columns, List<? extends Config> sort) {
    final List<OrderColumn> orders = req.getOrders();
    if (!Iterables.isEmpty(orders)) {
      // 排序
      Collections.sort(sort, new Comparator<Config>() {
        @Override
        public int compare(Config o1, Config o2) {
          int ret = 0;
          for (OrderColumn i : orders) {
            DataColumn col = columns.get(i.getColumn());
            if (!col.isOrderable()) {
              continue;
            }
            String data = col.getData();
            ret = AjaxController.this.compare(o1, o2, data);
            if (i.getDir().equals("desc")) {
              ret = -ret;
            }
            if (ret != 0)
              break;
          }
          return ret;
        }
      });
    }
  }

  private void highlight(List<? extends Config> configs, Set<String> words) {
    for (Config i : configs) {
      i.setSummary(Lists.newArrayList(words));
    }
  }

  private boolean found(Config c, String data, SearchColumn s) {
    if ((data.equals("summary") || data.equals("content")) && s.find(c.getContent())) {
      return true;
    } else if (data.equals("name") && s.find(c.getName())) {
      return true;
    } else if (data.equals("profile") && s.find(c.getProfile())) {
      return true;
    } else if (data.equals("editor") && s.find(c.getEditor())) {
      return true;
    }
    return false;
  }

  private int compare(Config o1, Config o2, String data) {
    int ret = 0;
    switch (data) {
      case "name":
        ret = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        break;
      case "profile":
        ret = o1.getProfile().compareTo(o2.getProfile());
        break;
      case "summary":
      case "content":
        ret = o1.getContent().compareTo(o2.getContent());
        break;
      case "version":
        ret = o1.getVersion() - o2.getVersion();
        break;
      case "editor":
        ret = o1.getEditor().compareTo(o2.getEditor());
        break;
      case "mtime":
        ret = o1.getModifyTime().compareTo(o2.getModifyTime());
        break;
    }
    return ret;
  }
}
