/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.yetus.audience.InterfaceAudience;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Performs multiple mutations atomically on a single row.
 * Currently {@link Put} and {@link Delete} are supported.
 *
 * The mutations are performed in the order in which they
 * were added.
 *
 * <p>We compare and equate mutations based off their row so be careful putting RowMutations
 * into Sets or using them as keys in Maps.
 */
@InterfaceAudience.Public
public class RowMutations implements Row {
  private final List<Mutation> mutations;
  private final byte [] row;

  public RowMutations(byte [] row) {
    this(row, -1);
  }
  /**
   * Create an atomic mutation for the specified row.
   * @param row row key
   * @param initialCapacity the initial capacity of the RowMutations
   */
  public RowMutations(byte [] row, int initialCapacity) {
    Mutation.checkRow(row);
    this.row = Bytes.copy(row);
    if (initialCapacity <= 0) {
      this.mutations = new ArrayList<>();
    } else {
      this.mutations = new ArrayList<>(initialCapacity);
    }
  }

  /**
   * Add a {@link Put} operation to the list of mutations
   * @param p The {@link Put} to add
   * @throws IOException
   */
  public void add(Put p) throws IOException {
    internalAdd(p);
  }

  /**
   * Add a {@link Delete} operation to the list of mutations
   * @param d The {@link Delete} to add
   * @throws IOException
   */
  public void add(Delete d) throws IOException {
    internalAdd(d);
  }

  private void internalAdd(Mutation m) throws IOException {
    int res = Bytes.compareTo(this.row, m.getRow());
    if (res != 0) {
      throw new WrongRowIOException("The row in the recently added Put/Delete <" +
          Bytes.toStringBinary(m.getRow()) + "> doesn't match the original one <" +
          Bytes.toStringBinary(this.row) + ">");
    }
    mutations.add(m);
  }

  /**
   * @deprecated As of release 2.0.0, this will be removed in HBase 3.0.0.
   *             Use {@link Row#COMPARATOR} instead
   */
  @Deprecated
  @Override
  public int compareTo(Row i) {
    return Bytes.compareTo(this.getRow(), i.getRow());
  }

  /**
   * @deprecated As of release 2.0.0, this will be removed in HBase 3.0.0.
   *             No replacement
   */
  @Deprecated
  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj instanceof RowMutations) {
      RowMutations other = (RowMutations)obj;
      return compareTo(other) == 0;
    }
    return false;
  }

  /**
   * @deprecated As of release 2.0.0, this will be removed in HBase 3.0.0.
   *             No replacement
   */
  @Deprecated
  @Override
  public int hashCode(){
    return Arrays.hashCode(row);
  }

  @Override
  public byte[] getRow() {
    return row;
  }

  /**
   * @return An unmodifiable list of the current mutations.
   */
  public List<Mutation> getMutations() {
    return Collections.unmodifiableList(mutations);
  }

  public int getMaxPriority() {
    int maxPriority = Integer.MIN_VALUE;
    for (Mutation mutation : mutations) {
      maxPriority = Math.max(maxPriority, mutation.getPriority());
    }
    return maxPriority;
  }
}
