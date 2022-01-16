import java.io.PrintStream;
import java.util.Optional;


public class Console {
    
    /**
     * The print stream to output to
     */
    private final PrintStream mStream;

    /**
     * The current progress bar
     */
    private Optional<ProgressBar> mProgressBar;

    /**
     * The current line length
     */
    private int mCurrentLineLength = 0;
    
    /**
     * Whether to display
     */
    private boolean mDisplayOutput = true;

    public Console() {
        this(System.out);
    }

    public Console(final PrintStream stream) {
        mStream = stream;
        mProgressBar = Optional.empty();
    }

    public void startProgressBar(final String name, final int maxValue) {
        mProgressBar = Optional.of(new ProgressBar(name, maxValue));
        print(mProgressBar.get());
    }

    public void updateProgressBar() {
        mProgressBar.ifPresent(progressBar -> {
            progressBar.increment();
            printR(progressBar);
        });
    }

    public void updateProgressBar(final String message) {
        mProgressBar.ifPresent(progressBar -> progressBar.setMessage(message));
        updateProgressBar();
    }

    public void postToProgessBar(final String message) {
        mProgressBar.ifPresent(progressBar -> {
            progressBar.setMessage(message);
            printR(progressBar);
        });
    }

    public void completeProgressBar() {
        mProgressBar.ifPresent(progressBar -> {
            progressBar.complete();
            printR(progressBar + "\n");
        });
    }

    public void notify(final String message) {

    }

    public void print(final Object obj) {
        print(obj.toString());
    }

    public void print(final String message) {
        if (mDisplayOutput) {
            mStream.print(message);
            mCurrentLineLength += message.length();
        }
    }

    public void printR(final Object obj) {
        printR(obj.toString());
    }

    public void printR(final String message) {
        carriageReturn();
        print(message);
    }

    public void println(final Object obj) {
        println(obj.toString());
    }

    public void println(final String message) {
        if (mDisplayOutput) {
            mStream.println(message);
            mCurrentLineLength = 0;
        }
    }

    private void carriageReturn() {
        if (mDisplayOutput) {
            mStream.print("\r");
            for (int i = 0; i < mCurrentLineLength / 10; ++i) {
                mStream.print("          ");
            }
            for (int i = 0; i < mCurrentLineLength % 10; ++i) {
                mStream.print(" ");
            }
            mStream.print("\r");
            mCurrentLineLength = 0;
        }
    }


    private class ProgressBar {

        private final String mTitle;
        private final int mMaxValue;
        private int mCurrentValue;
        private String mMessage;

        public ProgressBar(final String title, final int maxValue) {
            mTitle = title;
            mMessage = "";
            mMaxValue = maxValue;
            mCurrentValue = 0;
        }

        public void setMessage(final String message) {
            mMessage = message;
        }

        public void increment() {
            if (mCurrentValue < mMaxValue) {
                ++mCurrentValue;
            }
        }

        public void complete() {
            mCurrentValue = mMaxValue;
            mMessage = "";
        }

        public boolean isComplete() {
            return mCurrentValue == mMaxValue;
        }

        @Override
        public String toString() {
            final StringBuilder stringBuilder =
                new StringBuilder(mTitle.length() + 4 + mMaxValue + 3 + mMessage.length());
            
            stringBuilder.append(mTitle + ": [");
            
            for (int i = 0; i < mCurrentValue - 1; ++i) {
                stringBuilder.append("=");
            }

            if (isComplete()) {
                stringBuilder.append("=");
            } 
            else if (mCurrentValue > 0) {
                stringBuilder.append(">");
            }


            for (int i = mCurrentValue; i < mMaxValue; ++i) {
                stringBuilder.append(" ");
            }

            stringBuilder.append("]");

            if (!mMessage.isEmpty()) {
                stringBuilder.append(" - ");
                stringBuilder.append(mMessage);
            }

            return stringBuilder.toString();
        }
    }
}
