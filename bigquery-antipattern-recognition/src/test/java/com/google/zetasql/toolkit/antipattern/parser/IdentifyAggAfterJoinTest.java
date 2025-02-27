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

import static org.junit.Assert.assertEquals;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import org.junit.Before;
import org.junit.Test;

public class IdentifyAggAfterJoinTest {
  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void aggAfterJoinSimpleSelect() {
    String expected = "GROUP BY found at line number 10 after JOIN at line number 5. Consider applying aggregation before joining to reduce join data";
    String query =
        "select\n"
            + "  user_id,\n"
            + "  count(text) AS comments_count\n"
            + "from\n"
            + "  stackoverflow.comments as c\n"
            + "join\n"
            + "  stackoverflow.users as u\n"
            + "on c.user_id = u.id\n"
            + "and c.user_id_new = u.id_n\n"
            + "group by c.user_id";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyAggAfterJoin()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  @Test
  public void aggBeforeJoinSimpleSelect() {
    String expected = "";
    String query =
        "select\n"
            + "  user_id  \n"
            + "  comments_count\n"
            + "from (\n"
            + "  select \n"
            + "    user_id,\n"
            + "    count(text) AS comments_count  \n"
            + "  from `bigquery-public-data.stackoverflow.comments`\n"
            + "  group by user_id\n"
            + ") as c\n"
            + "join `bigquery-public-data.stackoverflow.users` as u\n"
            + "on c.user_id = u.id\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyAggAfterJoin()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  @Test
  public void aggAfterJoinInWithClause() {
    String expected = "GROUP BY found at line number 17 after JOIN at line number 5. Consider applying aggregation before joining to reduce join data";
    String query =
        "WITH\n"
            + "  users_posts AS (\n"
            + "  SELECT *\n"
            + "  FROM\n"
            + "    `bigquery-public-data`.stackoverflow.comments AS c\n"
            + "  JOIN\n"
            + "    `bigquery-public-data`.stackoverflow.users AS u\n"
            + "  ON\n"
            + "    c.user_id = u.id\n"
            + "  )\n"
            + "SELECT\n"
            + "  user_id,\n"
            + "  ANY_VALUE(display_name) AS display_name,\n"
            + "  ANY_VALUE(reputation) AS reputation,\n"
            + "  COUNT(text) AS comments_count\n"
            + "FROM users_posts\n"
            + "GROUP BY user_id\n"
            + "ORDER BY comments_count DESC\n"
            + "LIMIT 20;\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyAggAfterJoin()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  @Test
  public void aggBeforeJoinInWithClause() {
    String expected = "";
    String query =
        "WITH\n"
            + "  comments AS (\n"
            + "  SELECT\n"
            + "    user_id,\n"
            + "    COUNT(text) AS comments_count\n"
            + "  FROM\n"
            + "    `bigquery-public-data`.stackoverflow.comments\n"
            + "  WHERE\n"
            + "    user_id IS NOT NULL\n"
            + "  GROUP BY user_id\n"
            + "  ORDER BY comments_count DESC\n"
            + "  LIMIT 20\n"
            + "  )\n"
            + "SELECT\n"
            + "  user_id,\n"
            + "  display_name,\n"
            + "  reputation,\n"
            + "  comments_count\n"
            + "FROM comments\n"
            + "JOIN\n"
            + "  `bigquery-public-data`.stackoverflow.users AS u\n"
            + "ON\n"
            + "  user_id = u.id\n"
            + "ORDER BY comments_count DESC\n";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyAggAfterJoin()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  @Test
  public void aggAfterJoinInSubquery() {
    String expected = "GROUP BY found at line number 10 after JOIN at line number 7. Consider applying aggregation before joining to reduce join data";
    String query =
        "SELECT\n"
            + "    tt1.sum_col2, count(1) ct\n"
            + "FROM \n"
            + "    (SELECT \n"
            + "        t1.col1, sum(t2.col2) sum_col2\n"
            + "    FROM \n"
            + "        t1\n"
            + "    JOIN\n"
            + "        t2 ON t1.col1 = t2.col2\n"
            + "    GROUP BY\n"
            + "        t2.col2 ) tt1\n"
            + "GROUP BY    \n"
            + "    tt1.sum_col2";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyAggAfterJoin()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }

  @Test
  public void aggBeforeJoinInSubquery() {
    String expected = "";
    String query =
        "SELECT\n"
            + "    tt1.sum_col2, count(1) ct\n"
            + "FROM \n"
            + "(SELECT \n"
            + "col1,\n"
            + "sum_col2\n"
            + "FROM\n"
            + "t1 \n"
            + "JOIN\n"
            + "    ( SELECT \n"
            + "        col2,\n"
            + "        sum(col2) sum_col2\n"
            + "      FROM\n"
            + "      t2\n"
            + "      GROUP BY col2\n"
            + "    ) as t2_temp\n"
            + "ON\n"
            + "t1.col1=t2_temp.col2 ) tt1\n"
            + "GROUP BY    \n"
            + "tt1.sum_col2";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyAggAfterJoin()).run(parsedQuery, query);
    assertEquals(expected, recommendations);
  }



}
