package org.mariadb.jdbc.unit.util.constant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.util.constants.HaMode;

public class HaModeTest {
  @Test
  public void replicationEndOfBlacklistTest() {
    HostAddress host1 = HostAddress.from("1", 3306, true);
    HostAddress host2 = HostAddress.from("2", 3306, false);
    HostAddress host3 = HostAddress.from("3", 3306, false);

    List<HostAddress> available = new ArrayList<>();
    available.add(host1);
    available.add(host2);
    available.add(host3);

    ConcurrentMap<HostAddress, Long> denyList = new ConcurrentHashMap<>();
    denyList.putIfAbsent(host1, System.currentTimeMillis() - 10);

    Optional<HostAddress> res = HaMode.REPLICATION.getAvailableHost(available, denyList, true);
    Assertions.assertTrue(res.isPresent());
    Assertions.assertEquals(host1, res.get());

    res = HaMode.REPLICATION.getAvailableHost(available, denyList, false);
    Assertions.assertTrue(res.isPresent());
    Assertions.assertEquals(host2, res.get());

    denyList.putIfAbsent(host2, System.currentTimeMillis() - 10);
    res = HaMode.REPLICATION.getAvailableHost(available, denyList, false);
    Assertions.assertTrue(res.isPresent());
    Assertions.assertEquals(host2, res.get());

    denyList.putIfAbsent(host2, System.currentTimeMillis() + 1000);
    res = HaMode.REPLICATION.getAvailableHost(available, denyList, false);
    Assertions.assertTrue(res.isPresent());
    Assertions.assertEquals(host3, res.get());
  }

  @Test
  public void loadBalanceTest() {
    HostAddress host1 = HostAddress.from("1", 3306, true);
    HostAddress host2 = HostAddress.from("2", 3306, true);
    HostAddress host3 = HostAddress.from("3", 3306, true);
    HostAddress host4 = HostAddress.from("4", 3306, false);
    HostAddress host5 = HostAddress.from("5", 3306, false);
    HostAddress host6 = HostAddress.from("6", 3306, false);

    List<HostAddress> available = new ArrayList<>();
    available.add(host1);
    available.add(host2);
    available.add(host3);
    available.add(host4);
    available.add(host5);
    available.add(host6);

    ConcurrentMap<HostAddress, Long> denyList = new ConcurrentHashMap<>();
    Map<HostAddress, Integer> res = loopPercReturn(HaMode.LOADBALANCE, available, denyList, true);
    for (Map.Entry<HostAddress, Integer> entry : res.entrySet()) {
      System.out.println(entry.getKey() + " : " + entry.getValue());
    }
    Integer use = res.get(host1);
    Assertions.assertTrue(use > 250 && use < 400, "Expect 33% host1, 33% host2 and 33% host 3");

    denyList.putIfAbsent(host1, System.currentTimeMillis() + 10000);

    res = loopPercReturn(HaMode.LOADBALANCE, available, denyList, true);
    for (Map.Entry<HostAddress, Integer> entry : res.entrySet()) {
      System.out.println(entry.getKey() + " : " + entry.getValue());
    }
    use = res.get(host2);
    Assertions.assertTrue(use > 450 && use < 550, "Expect 50% host2 and 50% host 3");
  }

  @Test
  public void noneTest() {
    HostAddress host1 = HostAddress.from("1", 3306, true);

    List<HostAddress> available = new ArrayList<>();
    available.add(host1);

    ConcurrentMap<HostAddress, Long> denyList = new ConcurrentHashMap<>();

    Optional<HostAddress> res = HaMode.NONE.getAvailableHost(available, denyList, true);
    Assertions.assertTrue(res.isPresent());
    Assertions.assertEquals(host1, res.get());

    res = HaMode.NONE.getAvailableHost(new ArrayList<>(), denyList, true);
    Assertions.assertFalse(res.isPresent());
  }

  private Map<HostAddress, Integer> loopPercReturn(
      HaMode haMode,
      List<HostAddress> available,
      ConcurrentMap<HostAddress, Long> denyList,
      Boolean primary) {
    Map<HostAddress, Integer> resMap = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      Optional<HostAddress> res = haMode.getAvailableHost(available, denyList, primary);
      if (res.isPresent()) {
        if (resMap.containsKey(res.get())) {
          resMap.put(res.get(), resMap.get(res.get()) + 1);
        } else {
          resMap.put(res.get(), 1);
        }
      }
    }
    return resMap;
  }
}