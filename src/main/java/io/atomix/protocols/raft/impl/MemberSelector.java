/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.protocols.raft.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.atomix.protocols.raft.CommunicationStrategy;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cluster member selector.
 */
public final class MemberSelector implements Iterator<String> {

  /**
   * Address selector state.
   */
  public enum State {

    /**
     * Indicates that the selector has been reset.
     */
    RESET,

    /**
     * Indicates that the selector is being iterated.
     */
    ITERATE,

    /**
     * Indicates that selector iteration is complete.
     */
    COMPLETE

  }

  private String leader;
  private Set<String> members;
  private volatile String selection;
  private final CommunicationStrategy strategy;
  private Collection<String> selections;
  private Iterator<String> selectionsIterator;

  public MemberSelector(String leader, Collection<String> members, CommunicationStrategy strategy) {
    this.leader = leader;
    this.members = new LinkedHashSet<>(members);
    this.strategy = checkNotNull(strategy, "strategy cannot be null");
    this.selections = strategy.selectConnections(leader, Lists.newLinkedList(members));
  }

  /**
   * Returns the address selector state.
   *
   * @return The address selector state.
   */
  public State state() {
    if (selectionsIterator == null) {
      return State.RESET;
    } else if (hasNext()) {
      return State.ITERATE;
    } else {
      return State.COMPLETE;
    }
  }

  /**
   * Returns the current address selection.
   *
   * @return The current address selection.
   */
  public String current() {
    return selection;
  }

  /**
   * Returns the current selector leader.
   *
   * @return The current selector leader.
   */
  public String leader() {
    return leader;
  }

  /**
   * Returns the current set of servers.
   *
   * @return The current set of servers.
   */
  public Set<String> members() {
    return members;
  }

  /**
   * Resets the member iterator.
   *
   * @return The member selector.
   */
  public MemberSelector reset() {
    if (selectionsIterator != null) {
      this.selections = strategy.selectConnections(leader, Lists.newLinkedList(members));
      this.selectionsIterator = null;
    }
    return this;
  }

  /**
   * Resets the connection leader and members.
   *
   * @param members The collection of members.
   * @return The member selector.
   */
  public MemberSelector reset(String leader, Collection<String> members) {
    if (changed(leader, members)) {
      this.leader = leader != null && members.contains(leader) ? leader : null;
      this.members = Sets.newLinkedHashSet(members);
      this.selections = strategy.selectConnections(leader, Lists.newLinkedList(members));
      this.selectionsIterator = null;
    }
    return this;
  }

  /**
   * Returns a boolean value indicating whether the selector state would be changed by the given members.
   */
  private boolean changed(String leader, Collection<String> members) {
    checkNotNull(members, "members");
    checkArgument(!members.isEmpty(), "members cannot be empty");
    if (leader != null && !members.contains(leader)) {
      leader = null;
    }
    if (!Objects.equals(this.leader, leader)) {
      return true;
    } else if (!matches(this.members, members)) {
      return true;
    }
    return false;
  }

  /**
   * Returns a boolean value indicating whether the servers in the first list match the servers in the second list.
   */
  private boolean matches(Collection<String> left, Collection<String> right) {
    if (left.size() != right.size()) {
      return false;
    }

    for (String address : left) {
      if (!right.contains(address)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean hasNext() {
    return selectionsIterator == null ? !selections.isEmpty() : selectionsIterator.hasNext();
  }

  @Override
  public String next() {
    if (selectionsIterator == null) {
      selectionsIterator = selections.iterator();
    }
    String selection = selectionsIterator.next();
    this.selection = selection;
    return selection;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("strategy", strategy)
        .toString();
  }

}