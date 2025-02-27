/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zetasql.toolkit.antipattern.parser;

import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyAggAfterJoinVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifySimpleSelectStarVisitor;
import java.util.stream.Collectors;

/*
 * Identifies GROUP BY applied on a join key after the join has occurred
 */
public class IdentifyAggAfterJoin {

  public String run(ASTStatement parsedQuery, String query) {
    IdentifyAggAfterJoinVisitor visitor = new IdentifyAggAfterJoinVisitor(query);
    parsedQuery.accept(visitor);

    return visitor.getResult().stream().distinct().collect(Collectors.joining("\n"));
  }
}
