package com.xxxtai.main;

import com.xxxtai.model.Graph;
import com.xxxtai.view.DrawingGui;
import com.xxxtai.view.SchedulingGui;
import com.xxxtai.view.SetingGui;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@Component
@Slf4j
public class Main extends JFrame{

	private static final long serialVersionUID = 1L;
	@Resource
	private SchedulingGui schedulingGui;
	@Resource
	private SetingGui setingGui;
	@Resource
	private DrawingGui graphingGui;
	@Resource
	private Graph graph;

	public Main(){
		super("模拟多AGV场景");
		
		
		
		
		
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setExtendedState(Frame.MAXIMIZED_BOTH);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);		
		this.addWindowListener(new WindowAdapter() {
	          @Override
	          public void windowClosing(WindowEvent e){
	        	  //exit();
	          }
	    });
	}
	public void init(ApplicationContext context){
		graphingGui.getGuiInstance(Main.this, schedulingGui, setingGui, graphingGui);
		setingGui.getGuiInstance(Main.this, schedulingGui, setingGui, graphingGui);
		schedulingGui.getGuiInstance(Main.this, schedulingGui, setingGui, graphingGui);
		schedulingGui.init(context);
		
		
		this.getContentPane().add(schedulingGui);
		this.repaint();
		this.validate();
	}
	

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/spring/beans.xml");
		Main main = context.getBean(Main.class);
		main.init(context);
	}
	
	public void exit(){
		Object[] option = {"确认", "取消"};
		JOptionPane pane = new JOptionPane("确认关闭吗？", JOptionPane.QUESTION_MESSAGE, 
				JOptionPane.YES_NO_OPTION, null, option, option[1]);
		JDialog dialog = pane.createDialog(this, "警告");
		dialog.setVisible(true);
		Object result = pane.getValue();
		if(result == null || result == option[1]){
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}else if(result == option[0]){
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
	}
}