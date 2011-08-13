package com.raphfrk.bukkit.eventlink;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DeadlockMonitor extends KillableThread {

	public void run() {

		while(!killed()) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
			System.out.println("Checking for deadlocks");
			ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
			long[] ids = tmx.findDeadlockedThreads();
			if (ids != null) {
				ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
				System.out.println("The following threads are deadlocked:");
				for (ThreadInfo ti : infos) {
					System.out.println(ti);
				}
			}

		}

	}

}
