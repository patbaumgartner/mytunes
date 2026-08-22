package com.patbaumgartner.mytunes;

import com.patbaumgartner.mytunes.platform.BrowserConsole;
import com.patbaumgartner.mytunes.ui.PlayerUi;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * myTunes, entirely client side.
 * <p>
 * This {@code main} runs inside the browser: GraalVM Web Image compiles it to WebAssembly
 * and the generated loader invokes it once the module is instantiated. The Spring
 * {@code ApplicationContext} is refreshed in the browser tab, and the interface is then
 * built from Java against the live DOM. No Spring application runs on a server at any
 * point.
 * <p>
 * The excluded auto-configurations are the ones that are meaningless inside a browser
 * tab: there is no thread to execute or schedule tasks on, no SSL bundle to load, no AOP
 * proxy target, no availability probe to answer and no {@code git.properties} to serve.
 * Because Spring Boot's AOT step evaluates the exclusions at build time, the classes
 * behind them never become reachable and are dropped from the Wasm module.
 * {@code ConfigurationPropertiesAutoConfiguration} and
 * {@code PropertyPlaceholderAutoConfiguration} must not join this list: excluding them
 * changes the AOT-generated initializer set in a way that crashes the module at startup
 * in the browser with {@code ClassNotFoundException}.
 */
@SpringBootApplication(exclude = { AopAutoConfiguration.class, ApplicationAvailabilityAutoConfiguration.class,
		LifecycleAutoConfiguration.class, ProjectInfoAutoConfiguration.class, SslAutoConfiguration.class,
		TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class })
public class MyTunesApplication {

	public static void main(String[] args) {
		long started = System.currentTimeMillis();
		SpringApplication application = new SpringApplication(MyTunesApplication.class);
		application.setBannerMode(Banner.Mode.OFF);
		// Web Image is single threaded, so the hook thread cannot start, and a browser
		// page is discarded by the browser rather than through a JVM shutdown sequence.
		application.setRegisterShutdownHook(false);

		ConfigurableApplicationContext context = application.run(args);
		BrowserConsole.log("[mytunes] Spring Boot " + SpringBootVersion.getVersion() + " started in the browser in "
				+ (System.currentTimeMillis() - started) + "ms with " + context.getBeanDefinitionCount() + " beans");

		context.getBean(PlayerUi.class).start();
		BrowserConsole.log("[mytunes] interface ready");
	}

}
