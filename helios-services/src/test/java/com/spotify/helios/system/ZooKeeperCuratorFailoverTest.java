/**
 * Copyright (C) 2014 Spotify AB
 */

package com.spotify.helios.system;

import com.spotify.helios.ZooKeeperClusterTestManager;

import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertArrayEquals;

@RunWith(MockitoJUnitRunner.class)
public class ZooKeeperCuratorFailoverTest {

  @Rule public ExpectedException expectedException = ExpectedException.none();

  public static final byte[] FOO_DATA = "foo".getBytes();
  public static final String FOO = "/foo";

  private ZooKeeperClusterTestManager zk;

  @Before
  public void setup() {
    zk = new ZooKeeperClusterTestManager();
  }

  @After
  public void teardown() {
    zk.close();
  }

  @Test
  public void verifyCanCreateNodesWithOnePeerDead() throws Exception {
    zk.stopPeer(0);
    Thread.sleep(1000);

    try {
      zk.curator().create().forPath(FOO, FOO_DATA);
      assertArrayEquals(FOO_DATA, zk.curator().getData().forPath(FOO));
    } catch (KeeperException.NodeExistsException ignore) {
    }
  }

  @Test
  public void verifyCanNotCreateNodesWithTwoPeersDead() throws Exception {
    zk.stopPeer(0);
    zk.stopPeer(1);
    Thread.sleep(1000);

    expectedException.expect(KeeperException.ConnectionLossException.class);

    zk.curator().create().forPath(FOO, FOO_DATA);
    assertArrayEquals(FOO_DATA, zk.curator().getData().forPath(FOO));
  }


  @Test
  public void verifyZooKeeperRecoversWithTwoPeersAlive() throws Exception {
    zk.stopPeer(0);
    zk.stopPeer(1);
    Thread.sleep(1000);

    zk.startPeer(0);

    try {
      zk.curator().create().forPath(FOO, FOO_DATA);
      assertArrayEquals(FOO_DATA, zk.curator().getData().forPath(FOO));
    } catch (KeeperException.NodeExistsException ignore) {
    }
  }

  @Test
  public void verifyZooKeeperToleratesOneNodeDataLoss() throws Exception {
    try {
      zk.curator().create().forPath(FOO, FOO_DATA);
      assertArrayEquals(FOO_DATA, zk.curator().getData().forPath(FOO));
    } catch (KeeperException.NodeExistsException ignore) {
    }

    Thread.sleep(1000);

    zk.stopPeer(0);
    zk.resetPeer(0);
    zk.startPeer(0);

    zk.stopPeer(1);
    Thread.sleep(1000);

    assertArrayEquals(FOO_DATA, zk.curator().getData().forPath(FOO));
  }


}