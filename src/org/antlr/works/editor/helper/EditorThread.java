/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.antlr.works.editor.helper;

import org.antlr.works.util.CancelObject;

public abstract class EditorThread implements Runnable, CancelObject {

    protected int threadSleep = 100;
    protected int threshold = 0;
    protected boolean running = false;
    protected boolean run = false;
    protected Thread thread = null;
    protected boolean asleep = false;
    protected boolean skip = false;

    public synchronized void setRunning(boolean flag) {
        running = flag;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    protected synchronized void start() {
        this.threadSleep = Integer.MAX_VALUE;
        start_();
    }

    protected synchronized void start(int threadSleep) {
        this.threadSleep = threadSleep;
        start_();
    }

    private void start_() {
        if(!run) {
            run = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    public synchronized void skip() {
        skip = true;
    }

    private synchronized void resetSkip() {
        skip = false;
    }

    public synchronized void stop() {
        threshold = 0;
        thread.interrupt();
        run = false;
    }

    /** Interrupt the thread if it is sleeping. If threshold > 0, the thread will sleep an
     * additionnal 'threshold' ms until no other awakeThread() is called
     * before trying to compute something.
     *
     * @param threshold
     */

    public synchronized void awakeThread(int threshold) {
        resetSkip();
        this.threshold = threshold;
        if(asleep)
            thread.interrupt();
    }

    public boolean cancel() {
        return !run;
    }

    protected abstract void threadRun() throws Exception;

    public boolean threadSleep(int ms) {
        boolean interrupted = false;
        asleep = true;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            interrupted = true;
        }
        asleep = false;
        return interrupted;
    }

    public void run() {
        setRunning(true);
        while(run) {
            if(threadSleep(threadSleep)) {
                // Sleep interrupted. If threshold is > 0, then wait
                // this amount of time and loop until the thread is not
                // anymore interrupted.
                if(threshold>0) {
                    while(threadSleep(threshold)) {
                    }
                }
            }

            if(!run)
                break;

            if(skip) {
                resetSkip();
                continue;
            }

            try {
                threadRun();
            } catch(Exception e) {
                System.err.println("Exception in EditorThread: "+e);
                e.printStackTrace();
            }
        }
        setRunning(false);
    }

}
