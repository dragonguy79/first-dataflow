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
package com.example;

import com.example.complete.game.utils.WriteToBigQuery;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * An example that counts words in Shakespeare and includes Beam best practices.
 * <p>
 * <p>This class, {@link FitnessDataProcessor}, is the second in a series of four successively more detailed
 * 'word count' examples. You may first want to take a look at {@link MinimalWordCount}.
 * After you've looked at this example, then see the {@link DebuggingWordCount}
 * pipeline, for introduction of additional concepts.
 * <p>
 * <p>For a detailed walkthrough of this example, see
 * <a href="https://beam.apache.org/get-started/wordcount-example/">
 * https://beam.apache.org/get-started/wordcount-example/
 * </a>
 * <p>
 * <p>Basic concepts, also in the MinimalWordCount example:
 * Reading text files; counting a PCollection; writing to text files
 * <p>
 * <p>New Concepts:
 * <pre>
 *   1. Executing a Pipeline both locally and using the selected runner
 *   2. Using ParDo with static DoFns defined out-of-line
 *   3. Building a composite transform
 *   4. Defining your own pipeline options
 * </pre>
 * <p>
 * <p>Concept #1: you can execute this pipeline either locally or using by selecting another runner.
 * These are now command-line options and not hard-coded as they were in the MinimalWordCount
 * example.
 * <p>
 * <p>To change the runner, specify:
 * <pre>{@code
 *   --runner=YOUR_SELECTED_RUNNER
 * }
 * </pre>
 * <p>
 * <p>To execute this pipeline, specify a local output file (if using the
 * {@code DirectRunner}) or output prefix on a supported distributed file system.
 * <pre>{@code
 *   --output=[YOUR_LOCAL_FILE | YOUR_OUTPUT_PREFIX]
 * }</pre>
 * <p>
 * <p>The input file defaults to a public data set containing the text of of King Lear,
 * by William Shakespeare. You can override it and choose your own input with {@code --inputFile}.
 */
public class FitnessDataProcessor {

    /**
     * A PTransform that converts a PCollection containing lines of text into a PCollection of
     * FitnessDao.
     */
    public static class BuildFitnessDataMap extends PTransform<PCollection<String>,
            PCollection<FitnessDao>> {
        @Override
        public PCollection<FitnessDao> expand(PCollection<String> lines) {

            // Convert lines of text into individual words.
            PCollection<FitnessDao> daos = lines.apply(
                    ParDo.of(new BuildFitnessDaoFn()));

            return daos;
        }
    }

    /**
     * Function to build FitnessDao from a line of data
     */
    static class BuildFitnessDaoFn extends DoFn<String, FitnessDao> {

        @ProcessElement
        public void processElement(ProcessContext c) {
            // Split the line into words.
            String[] words = c.element().split(",");
            if (words.length != 13) {
                System.out.println("!!!!! fitness record does not contain 13 fields");
            }

            FitnessDao dao = new FitnessDao(
                    words[0], words[1], words[2], words[3], words[4], toInteger(words[5]), toFloat(words[6]),
                    words[7], toFloat(words[8]), toInteger(words[9]), toInteger(words[10]), toFloat(words[11]), toInteger(words[12]));
            c.output(dao);
        }
    }

    private static Integer toInteger(String dateStr)  {
        return Integer.parseInt(dateStr);
    }

    private static Float toFloat(String dateStr)  {
        return Float.parseFloat(dateStr);
    }

    /**
     * Print data to console
     */
    public static class PrintTextFn extends SimpleFunction<FitnessDao, FitnessDao> {
        @Override
        public FitnessDao apply(FitnessDao input) {
            System.out.println(input.toString());
            return input;
        }
    }

    /**
     * Options supported by {@link FitnessDataProcessor}.
     * <p>
     * <p>Concept #4: Defining your own configuration options. Here, you can add your own arguments
     * to be processed by the command-line parser, and specify default values for them. You can then
     * access the options values in your pipeline code.
     * <p>
     * <p>Inherits standard configuration options.
     */
    public interface FitnessDataOptions extends PipelineOptions {

        /**
         * By default, this example reads from a public dataset containing the text of
         * King Lear. Set this option to choose a different input file or glob.
         */
        @Description("Path of the file to read from")
        @Default.String("gs://dataflow-gym-bucket/fitness-data/*")
        String getInputFile();
        void setInputFile(String value);

        @Description("BigQuery dataset")
        @Required
        String getDataset();
        void setDataset(String value);

        @Description("BigQuery table")
        @Required
        String getTable();
        void setTable(String value);

    }

    protected static Map<String, WriteToBigQuery.FieldInfo<FitnessDao>> configureBigQueryWrite() {
        Map<String, WriteToBigQuery.FieldInfo<FitnessDao>> tableConfigure = new HashMap<>();
        tableConfigure.put("memberId", new WriteToBigQuery.FieldInfo<FitnessDao>("STRING", (c, w) -> c.element().getMemberId()));
        tableConfigure.put("firstName", new WriteToBigQuery.FieldInfo<FitnessDao>("STRING", (c, w) -> c.element().getFirstName()));
        tableConfigure.put("lastName", new WriteToBigQuery.FieldInfo<FitnessDao>("STRING", (c, w) -> c.element().getLastName()));
        tableConfigure.put("birthday", new WriteToBigQuery.FieldInfo<FitnessDao>("DATE", (c, w) -> c.element().getBirthday()));
        tableConfigure.put("gender", new WriteToBigQuery.FieldInfo<FitnessDao>("STRING", (c, w) -> c.element().getGender()));
        tableConfigure.put("height", new WriteToBigQuery.FieldInfo<FitnessDao>("INTEGER", (c, w) -> c.element().getHeight()));
        tableConfigure.put("weight", new WriteToBigQuery.FieldInfo<FitnessDao>("FLOAT", (c, w) -> c.element().getWeight()));
        tableConfigure.put("metricDate", new WriteToBigQuery.FieldInfo<FitnessDao>("DATE", (c, w) -> c.element().getMetricDate()));
        tableConfigure.put("hourSleep", new WriteToBigQuery.FieldInfo<FitnessDao>("FLOAT", (c, w) -> c.element().getHourSleep()));
        tableConfigure.put("caloriesConsumed", new WriteToBigQuery.FieldInfo<FitnessDao>("INTEGER", (c, w) -> c.element().getCaloriesConsumed()));
        tableConfigure.put("caloriesBurned", new WriteToBigQuery.FieldInfo<FitnessDao>("INTEGER", (c, w) -> c.element().getCaloriesBurned()));
        tableConfigure.put("bmi", new WriteToBigQuery.FieldInfo<FitnessDao>("FLOAT", (c, w) -> c.element().getBmi()));
        tableConfigure.put("heartRate", new WriteToBigQuery.FieldInfo<FitnessDao>("INTEGER", (c, w) -> c.element().getHeartRate()));
        return tableConfigure;
    }

    public static void main(String[] args) {
        FitnessDataOptions options = PipelineOptionsFactory.fromArgs(args).withValidation()
                .as(FitnessDataOptions.class);
        Pipeline p = Pipeline.create(options);

        // Concepts #2 and #3: Our pipeline applies the composite CountWords transform, and passes the
        // static FormatAsTextFn() to the ParDo transform.
        p.apply("ReadLines", TextIO.read().from(options.getInputFile()))
                .apply("BuildFitnessDAO", new BuildFitnessDataMap())
                .apply("LogMetrics", MapElements.via(new PrintTextFn()))
                .apply("WriteFitnessMetricToBigQuery",
                        new WriteToBigQuery<>(
                                options.as(GcpOptions.class).getProject(),
                                options.getDataset(),
                                options.getTable(),
                                configureBigQueryWrite()));

        p.run().waitUntilFinish();
    }

    /**
     * Class to hold fitness metric of 1 member of 1 day
     */
    @DefaultCoder(AvroCoder.class)
    static class FitnessDao {
        private String memberId;
        @Nullable
        private String firstName;
        @Nullable
        private String lastName;
        @Nullable
        private String birthday;
        @Nullable
        private String gender;
        @Nullable
        private Integer height;
        @Nullable
        private Float weight;
        @Nullable
        private String metricDate;
        @Nullable
        private Float hourSleep;
        @Nullable
        private Integer caloriesConsumed;
        @Nullable
        private Integer caloriesBurned;
        @Nullable
        private Float bmi;
        @Nullable
        private Integer heartRate;

        public FitnessDao() {
        }

        public FitnessDao(String memberId, String firstName, String lastName, String birthday, String gender, Integer height, Float weight, String metricDate, Float hourSleep, Integer caloriesConsumed, Integer caloriesBurned, Float bmi, Integer heartRate) {
            this.memberId = memberId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.birthday = birthday;
            this.gender = gender;
            this.height = height;
            this.weight = weight;
            this.metricDate = metricDate;
            this.hourSleep = hourSleep;
            this.caloriesConsumed = caloriesConsumed;
            this.caloriesBurned = caloriesBurned;
            this.bmi = bmi;
            this.heartRate = heartRate;
        }

        public String getMemberId() {
            return memberId;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getBirthday() {
            return birthday;
        }

        public String getGender() {
            return gender;
        }

        public Integer getHeight() {
            return height;
        }

        public Float getWeight() {
            return weight;
        }

        public String getMetricDate() {
            return metricDate;
        }

        public Float getHourSleep() {
            return hourSleep;
        }

        public Integer getCaloriesConsumed() {
            return caloriesConsumed;
        }

        public Integer getCaloriesBurned() {
            return caloriesBurned;
        }

        public Float getBmi() {
            return bmi;
        }

        public Integer getHeartRate() {
            return heartRate;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("================================================" + System.getProperty("line.separator"));
            sb.append("memberId: " + memberId + System.getProperty("line.separator"));
            sb.append("firstName: " + firstName + System.getProperty("line.separator"));
            sb.append("lastName: " + lastName + System.getProperty("line.separator"));
            sb.append("birthday: " + birthday + System.getProperty("line.separator"));
            sb.append("gender: " + gender + System.getProperty("line.separator"));
            sb.append("height: " + height + System.getProperty("line.separator"));
            sb.append("weight: " + weight + System.getProperty("line.separator"));
            sb.append("metricDate: " + metricDate + System.getProperty("line.separator"));
            sb.append("hourSleep: " + hourSleep + System.getProperty("line.separator"));
            sb.append("caloriesConsumed: " + caloriesConsumed + System.getProperty("line.separator"));
            sb.append("caloriesBurned: " + caloriesBurned + System.getProperty("line.separator"));
            sb.append("bmi: " + bmi + System.getProperty("line.separator"));
            sb.append("heartRate: " + heartRate);
            return sb.toString();
        }

    }
}
