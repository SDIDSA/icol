package icol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ThreadedTask<T> {
	private List<T> input;
	private Consumer<T> task;
	private BiConsumer<Integer, Integer> onItem;
	private int threadCount;

	public ThreadedTask(List<T> input, Consumer<T> task, BiConsumer<Integer, Integer> onItem, int threadCount) {
		this.input = input;
		this.task = task;
		this.onItem = onItem;
		this.threadCount = threadCount;
	}

	@SuppressWarnings("unchecked")
	public void execute() {
		ArrayList<Thread> ths = new ArrayList<>();

		int[] treated = new int[] { 0 };

		ArrayList<T>[] temp = new ArrayList[1];
		temp[0] = new ArrayList<>();

		input.forEach(icon -> {
			temp[0].add(icon);

			if (temp[0].size() >= (input.size() / threadCount)) {
				final ArrayList<T> fTemp = temp[0];
				treat(fTemp, ths, treated);
				temp[0] = new ArrayList<>();
			}
		});

		if (!temp[0].isEmpty()) {
			final ArrayList<T> fTemp = temp[0];
			treat(fTemp, ths, treated);
		}

		ths.forEach(Thread::start);
		ths.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				Thread.currentThread().interrupt();
			}
		});
	}

	private void treat(List<T> temp, ArrayList<Thread> ths, int[] treated) {
		Thread th = new Thread(() -> temp.forEach(i -> {
			treated[0]++;
			Platform.runLater(() -> onItem.accept(treated[0], input.size()));
			task.accept(i);
		}));
		ths.add(th);
	}
}
