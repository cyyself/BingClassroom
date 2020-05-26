# BingClassroom

学校的Java大作业

## 实验目的
运用面向对象程序设计思想，基于java的GUI、套接字和多线程机制，实现基绘图板程序。

## 实验项目内容
1. [x] 借鉴 实验一的类的架构构建绘图类集合，shape、shape类的派生类和graphics类；

2. [x] 将实验一 的OpenGLApp改为Jframe类的派生类，包含Graphics类实例作为数据成员，利用特定的布局管理器（layout）构建主窗口，在窗口中创建相应的控件允许用户选择当前绘制图形的形状、线条颜色、填充颜色等；

3. [x] 添加相应的事件监听和相应方法（处理用户的输入），例如绘制图形的形状、线条颜色、填充颜色的选择变化；

4. [x] 重载主窗口的paintComponent（Graphics g）方法，用于在主窗口中绘制Graphics类实例的图形数据，绘制过程中调用这些图形数据自己的draw()方法，可以把主窗口的g 传给每个图形元素类的 draw方法 ；

5. [x] 在主窗口中注册的MouseListener接口中相关处理方法，根据当前的用户选择实现类似window附件中painter工具的功能，例如：绘制指定图形和移动图形。

6. [x] 利用sock、多线程机制，修改程序结构实现一个基于java sock的网络白板程序(多个用户协同白板绘图程序)的设计与实现。

## 为什么起这个名字？

雨课堂->雷课堂->靐课堂

## 服务端程序

[BingClassroomServer](https://github.com/cyyself/BingClassroomServer)
