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
package org.apache.flink.table.planner.plan.rules.physical.stream

import org.apache.flink.legacy.table.sources.StreamTableSource
import org.apache.flink.table.planner.plan.nodes.FlinkConventions
import org.apache.flink.table.planner.plan.nodes.logical.FlinkLogicalLegacyTableSourceScan
import org.apache.flink.table.planner.plan.nodes.physical.stream.StreamPhysicalLegacyTableSourceScan
import org.apache.flink.table.planner.plan.schema.LegacyTableSourceTable

import org.apache.calcite.plan.{RelOptRule, RelOptRuleCall, RelTraitSet}
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.convert.ConverterRule
import org.apache.calcite.rel.convert.ConverterRule.Config
import org.apache.calcite.rel.core.TableScan

/**
 * Rule that converts [[FlinkLogicalLegacyTableSourceScan]] to
 * [[StreamPhysicalLegacyTableSourceScan]].
 */
class StreamPhysicalLegacyTableSourceScanRule(config: Config) extends ConverterRule(config) {

  /** Rule must only match if TableScan targets a [[StreamTableSource]] */
  override def matches(call: RelOptRuleCall): Boolean = {
    val scan: TableScan = call.rel(0)
    val tableSourceTable = scan.getTable.unwrap(classOf[LegacyTableSourceTable[_]])
    tableSourceTable match {
      case tst: LegacyTableSourceTable[_] =>
        tst.tableSource match {
          case _: StreamTableSource[_] => true
          case _ => false
        }
      case _ => false
    }
  }

  def convert(rel: RelNode): RelNode = {
    val scan = rel.asInstanceOf[FlinkLogicalLegacyTableSourceScan]
    val traitSet: RelTraitSet = rel.getTraitSet.replace(FlinkConventions.STREAM_PHYSICAL)

    new StreamPhysicalLegacyTableSourceScan(
      rel.getCluster,
      traitSet,
      scan.getHints,
      scan.getTable.asInstanceOf[LegacyTableSourceTable[_]]
    )
  }
}

object StreamPhysicalLegacyTableSourceScanRule {
  val INSTANCE: RelOptRule = new StreamPhysicalLegacyTableSourceScanRule(
    Config.INSTANCE.withConversion(
      classOf[FlinkLogicalLegacyTableSourceScan],
      FlinkConventions.LOGICAL,
      FlinkConventions.STREAM_PHYSICAL,
      "StreamPhysicalLegacyTableSourceScanRule"))
}
