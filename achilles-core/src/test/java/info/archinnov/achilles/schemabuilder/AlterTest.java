/*
 * Copyright (C) 2012-2014 DuyHai DOAN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package info.archinnov.achilles.schemabuilder;

import static info.archinnov.achilles.schemabuilder.TableOptions.Caching.ROWS_ONLY;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import com.datastax.driver.core.DataType;
import info.archinnov.achilles.schemabuilder.SchemaBuilder;
import info.archinnov.achilles.schemabuilder.TableOptions;

public class AlterTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void should_alter_column_type() throws Exception {
        //When
        final String built = SchemaBuilder.alterTable("test").alterColumn("name").type(DataType.ascii());

        //Then
        assertThat(built).isEqualTo("\n\tALTER TABLE test ALTER name TYPE ascii");
    }

    @Test
    public void should_alter_column_type_with_keyspace() throws Exception {
        //When
        final String built = SchemaBuilder.alterTable("ks", "test").alterColumn("name").type(DataType.ascii());

        //Then
        assertThat(built).isEqualTo("\n\tALTER TABLE ks.test ALTER name TYPE ascii");
    }

    @Test
    public void should_add_column() throws Exception {
        //When
        final String built = SchemaBuilder.alterTable("test").addColumn("location").type(DataType.ascii());

        //Then
        assertThat(built).isEqualTo("\n\tALTER TABLE test ADD location ascii");
    }

    @Test
    public void should_rename_column() throws Exception {
        //When
        final String built = SchemaBuilder.alterTable("test").renameColumn("name").to("description");

        //Then
        assertThat(built).isEqualTo("\n\tALTER TABLE test RENAME name TO description");
    }

    @Test
    public void should_drop_column() throws Exception {
        //When
        final String built = SchemaBuilder.alterTable("test").dropColumn("name");

        //Then
        assertThat(built).isEqualTo("\n\tALTER TABLE test DROP name");
    }

    @Test
    public void should_alter_table_options() throws Exception {
        //When
        final String built = SchemaBuilder.alterTable("test").withOptions()
                .bloomFilterFPChance(0.01)
                .caching(ROWS_ONLY)
                .comment("This is a comment")
                .compactionOptions(TableOptions.CompactionOptions.leveledStrategy().ssTableSizeInMB(160))
                .compressionOptions(TableOptions.CompressionOptions.lz4())
                .dcLocalReadRepairChance(0.21)
                .defaultTimeToLive(100)
                .gcGraceSeconds(9999L)
                .indexInterval(512)
                .memtableFlushPeriodInMillis(12L)
                .populateIOOnCacheFlush(true)
                .replicateOnWrite(true)
                .speculativeRetry(TableOptions.SpeculativeRetryValue.always())
                .build();

        //Then
        assertThat(built).isEqualTo("\n\tALTER TABLE test " +
                "WITH caching = 'rows_only' " +
                "AND bloom_filter_fp_chance = 0.01 " +
                "AND comment = 'This is a comment' " +
                "AND compression = {'sstable_compression' : 'LZ4Compressor'} " +
                "AND compaction = {'class' : 'LeveledCompactionStrategy', 'sstable_size_in_mb' : 160} " +
                "AND dclocal_read_repair_chance = 0.21 " +
                "AND default_time_to_live = 100 " +
                "AND gc_grace_seconds = 9999 " +
                "AND index_interval = 512 " +
                "AND memtable_flush_period_in_ms = 12 " +
                "AND populate_io_cache_on_flush = true " +
                "AND replicate_on_write = true " +
                "AND speculative_retry = 'ALWAYS'");
    }

    @Test
    public void should_fail_if_keyspace_name_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The keyspace name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("add","test")
                .addColumn("test").type(DataType.ascii());
    }

    @Test
    public void should_fail_if_table_name_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The table name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("add")
                .addColumn("test").type(DataType.ascii());
    }

    @Test
    public void should_fail_if_added_column_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The new column name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("test")
                .addColumn("add").type(DataType.ascii());
    }

    @Test
    public void should_fail_if_altered_column_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The altered column name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("test")
                .alterColumn("add").type(DataType.ascii());
    }

    @Test
    public void should_fail_if_renamed_column_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The renamed column name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("test")
                .renameColumn("add");
    }

    @Test
    public void should_fail_if_new_renamed_column_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The new column name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("test")
                .renameColumn("col").to("add");
    }

    @Test
    public void should_fail_if_drop_column_is_a_reserved_keyword() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The dropped column name 'add' is not allowed because it is a reserved keyword");

        SchemaBuilder.alterTable("test")
                .dropColumn("add");
    }

    @Test
    public void should_add_static_column() throws Exception {
        //When
        final String alterTable = SchemaBuilder.alterTable("test").addStaticColumn("stat").type(DataType.text());

        //Then
        assertThat(alterTable).isEqualTo("\n\tALTER TABLE test ADD stat text static");
    }
}
