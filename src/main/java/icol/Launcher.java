package icol;

import javafx.application.Application;

public class Launcher {
	public static void main(String[] args) {
		System.out.println(AdminCheck.IS_RUNNING_AS_ADMINISTRATOR);
		 Application.launch(Icol.class, args);
	}
}
