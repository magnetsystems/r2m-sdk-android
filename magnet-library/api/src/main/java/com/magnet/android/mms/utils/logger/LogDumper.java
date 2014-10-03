/*
 * Copyright (c) 2014 Magnet Systems, Inc.
 * All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.magnet.android.mms.utils.logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import java.io.FileReader;
//import java.io.PrintWriter;

/**
 * This class dumps the Android logcat entries bounded by a process.  It filters
 * the log by process ID's, and does not filter them by tags.  This utility class heavily depends on
 * the output format of logcat, and requires the <code>android.permission.READ_LOGS</code>
 * permission.
 */
public class LogDumper {
  /** Specifies the threadtime log output format (date, invocation time, priority/tag, and PID). */
  public final static String FORMAT_THREADTIME = "threadtime";
  /** Specifies the thread log output format (priority, tag, PID, TID). */
  public final static String FORMAT_THREAD = "thread";
  /** Specifies the process log output format (PID). */
  public final static String FORMAT_PROCESS = "process";
  /** Specifies the brief log output format (priority/tag and PID). */
  public final static String FORMAT_BRIEF = "brief";
  /** Specifies the threadtime log output format (date, invocation time, priority, tag, PID, TID). */
  public final static String FORMAT_TIME = "time";
    
  private LogDumper() {
    // Nothing
  }
  
  /**
   * Dump the logcat entries for the current process.  Each line of the log is
   * terminated by LF.
   * @param format The log format: {@link #FORMAT_BRIEF}, {@link #FORMAT_PROCESS}, 
   *  {@link #FORMAT_THREADTIME}, {@link #FORMAT_THREAD}, {@link #FORMAT_TIME}
   * @param out Output writer for the log entries.
   * @return Number of records found, or -1 if an error occurred.
   * @see #dump(String, int, Writer)
   */
  public static int dump(String format, Writer out) {
    return dump(format, android.os.Process.myPid(), out);
  }
  
  /**
   * Dump the logcat entries for a process ID.  Each line of the log is
   * terminated by LF.
   * @param format The log format: {@link #FORMAT_BRIEF}, {@link #FORMAT_PROCESS}, 
   *  {@link #FORMAT_THREADTIME}, {@link #FORMAT_THREAD}, {@link #FORMAT_TIME}
   * @param pid A PID whose value is greater than 0.
   * @param out Output writer for the log entries.
   * @return Number of records found, or -1 if an error occurred.
   * @see #getLatestPid(String)
   * @see #getPids(String)
   * @see #dump(String, int[], Writer)
   */
  public static int dump(String format, int pid, Writer out) {
    return dump(format, new int[] { pid }, out);
  }
  
  /**
   * Dump the logcat entries for a set of process IDs or all processes.  Each
   * line of the log is terminated by a newline.
   * @param format The log format: {@link #FORMAT_BRIEF}, {@link #FORMAT_PROCESS}, 
   *  {@link #FORMAT_THREADTIME}, {@link #FORMAT_THREAD}, {@link #FORMAT_TIME}
   * @param pids null for all processes, or an array of PIDs.
   * @param out Output writer for the log entries.
   * @return Number of records found, or -1 if an error occurred.
   * @see #getPids(String)
   */
  public static int dump(String format, int[] pids, Writer out) {
    if (pids != null && pids.length == 0)
      return 0;
    Process p = null;
    BufferedReader rd = null;
    PrintWriter wr = new PrintWriter(out);
    try {
      p = Runtime.getRuntime().exec(
          new String[] { "logcat", "-d",
              "-v", format,
              "-b", "main",
              "*:V" });
      rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
//      rd = new BufferedReader(new FileReader(format+".txt"));
      Pattern pattern = (pids == null) ? null : getPattern(format, pids);
      int numLines = 0;
      String line;
      while ((line = rd.readLine()) != null) {
        if (pattern != null && !pattern.matcher(line).find()) {
          continue;
        }
        ++numLines;
        wr.println(line);
      }
      wr.flush();
      return numLines;
    } catch (Throwable e) {
      e.printStackTrace();
      return -1;
    } finally {
      if (rd != null) {
        try {
          rd.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
      if (p != null) {
        try {
          p.waitFor();
        } catch (InterruptedException e) {
          // Ignored.
        }
        p.destroy();
      }
    }
  }
  
  // pids must have at least one element.
  private static String toList(int[] pids) {
    StringBuilder sb = new StringBuilder();
    for (int pid : pids) {
      if (sb.length() > 0) {
        sb.append('|');
      }
      sb.append(pid);
    }
    return sb.toString();
  }
  
  // The patterns must match the output from logcat.
  // pids must have at least one element.
  private static Pattern getPattern(String format, int[] pids) {
    String pidList = toList(pids);
    if (format.equals(FORMAT_THREADTIME)) {
      //01-21 11:30:14.015 19806 20621 I ServiceDumpSys: dumping service [account]
      return Pattern.compile(".{18}[ ]+"+pidList+"[ ]+.+");
    } else if (format.equals(FORMAT_THREAD)) {
      //I(19806:23478) dumping service [account]
      return Pattern.compile(".{2}[ ]*"+pidList+":.+");
    } else if (format.equals(FORMAT_PROCESS)) {
      //I(19806) dumping service [account]  (ServiceDumpSys)
      return Pattern.compile(".{2}[ ]*"+pidList+"\\).+");
    } else if (format.equals(FORMAT_BRIEF)) {
      //I/ServiceDumpSys(19806): dumping service [account]
      return Pattern.compile(".+\\([ ]*"+pidList+"\\).+");
    } else if (format.equals(FORMAT_TIME)) {
      //01-21 13:00:14.458 I/ServiceDumpSys(19806): dumping service [account]
      return Pattern.compile(".+\\([ ]*"+pidList+"\\).+");
    } else {
      return null;
    }
  }
  
  /**
   * Get the latest process ID for a tag.
   * @param tag The tag used in <code>"logcat tag:priority"</code>.
   * @return -1 for errors, 0 if no tag found, or a PID.
   */
  public static int getLatestPid(String tag) {
    int[] pids = getPids(tag);
    if (pids == null)
      return -1;
    return (pids.length == 0) ? 0 : pids[pids.length-1];
  }
  
  /**
   * Get all process IDs for a tag.  The PIDs are in the order in which log 
   * entries were inserted.
   * @param tag The tag used in <code>"logcat tag:priority"</code>.
   * @return null for errors, an empty array if no tag found, or an array of PIDs.
   */
  public static int[] getPids(String tag) {
    LinkedHashSet<Integer> pids = new LinkedHashSet<Integer>();
    Process p = null;
    BufferedReader rd = null;
    try {
      p = Runtime.getRuntime().exec(
          new String[] { "logcat", "-d", "-s",
              "-v", FORMAT_BRIEF,
              "-b", "main",
              tag+":V" });
      rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
      //I/ServiceDumpSys(19806): dumping service [account]
      Pattern pattern = Pattern.compile(".{2}"+tag+"\\([ ]*([0-9]+)\\).+");
      String line;
      Matcher matcher = null;
      while ((line = rd.readLine()) != null) {
        if (pattern != null && !((matcher = pattern.matcher(line)).find())) {
          continue;
        }
        if (matcher != null && matcher.groupCount() >= 1) {
          pids.add(Integer.valueOf(matcher.group(1)));
        }
      }
      int i = 0;
      int[] pa = new int[pids.size()];
      for (Integer pid : pids) {
        pa[i++] = pid.intValue();
      }
      return pa;
    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    } finally {
      try {
        rd.close();
      } catch (IOException e) {
        // Ignored.
      }
      if (p != null) {
        try {
          p.waitFor();
        } catch (InterruptedException e) {
          // Ignored.
        }
        p.destroy();
      }
    }
  }

//  public static void main(String[] args) {
//    if (args.length != 2) {
//      System.err.println("Usage: LogDumper format {tag|pid}");
//      System.exit(1);
//    }
//    if (Character.isDigit(args[1].charAt(0))) {
//      int pid = Integer.parseInt(args[1]);
//      LogDumper.dump(args[0], pid, new PrintWriter(System.out));
//    } else {
//      int[] pids = LogDumper.getPids(args[1]);
//      LogDumper.dump(args[0], pids, new PrintWriter(System.out));
//    }
//  }
}
