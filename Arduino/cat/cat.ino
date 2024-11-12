#include "U8glib.h"  // U8g2 库
#include <SoftwareSerial.h>
#include <Servo.h>  // 引入 Servo 库

SoftwareSerial espSerial(4, 5);  // RX, TX
U8GLIB_SSD1306_128X64 u8g(U8G_I2C_OPT_NONE);  // OLED 显示初始化

Servo myservo;  // 创建 Servo 对象，控制舵机
int pos = 0;
int SG90 = 9;
bool servoState = false; // 默认关闭状态，表示逆时针旋转


/////以下为函数声明  
extern unsigned long HX711_Read(void);
extern long Get_Weight();
///变量定义
float Weight = 0;
int HX711_SCK =2;   ///     作为输出口
int HX711_DT= 3;    ///     作为输入口
long HX711_Buffer = 0;
long Weight_Maopi = 0, Weight_Shiwu = 0;
#define GapValue 100       ///该值需校准 每个传感器都有所不同


void setup() {
  Serial.begin(9600);       // 初始化主串口
  espSerial.begin(9600);    // 初始化 ESP8266 串口
  delay(2000);  // 等待模块启动

  // 发送 AT 指令并检查响应
  espSerial.println("AT\r"); // 测试是否能通信
  delay(1000);
  readEspResponse();
  
  // 设置为 AP 模式
  espSerial.println("AT+CWMODE=2\r");  // 设置为 AP 模式
  delay(1000);
  readEspResponse();
  
  // 创建热点
  espSerial.println("AT+CWSAP=\"Next\",\"12345678\",5,3\r");  // 创建热点
  delay(1000);
  readEspResponse();

   //开启多连接
  espSerial.println("AT+RST\r");  
  delay(4000);
  readEspResponse();

  //开启多连接
  espSerial.println("AT+CIPMUX=1\r");  
  delay(1000);
  readEspResponse();

  //开启TCP服务器，端口号设置成8080
  espSerial.println("AT+CIPSERVER=1,8080\r");  
  delay(4000);
  readEspResponse();
     
    //开启TCP服务器，端口号设置成8080
  espSerial.println("AT+CIFSR\r");  
  delay(1000);
  readEspResponse();

     //开启TCP服务器，端口号设置成8080
  //espSerial.println("AT+CIPSEND=?\r");  
  //delay(3000);
  //readEspResponse();
  

   espSerial.println("AT+CIPSEND=0,20\r");  
  delay(1000);
  //readEspResponse();
    //初始化HX711的两个io口       
  pinMode(HX711_SCK, OUTPUT);  ///SCK 为输出口 ---输出脉冲
  pinMode(HX711_DT, INPUT); ///  DT为输入口  ---读取数据
  Serial.print("Welcome to use!\n");

  delay(3000);    ///延时3秒  
  //获取毛皮重量
  Weight_Maopi = HX711_Read(); 
  //rotateServoClockwise();
}

void draw() {
  u8g.setFont(u8g_font_fub14);  // 设置字体和字号
  u8g.setPrintPos(18, 32);  // 设置打印位置

  char weightStr[10];  // 用来存储转换后的字符串
  dtostrf(float(Weight / 1000), 4, 2, weightStr);  // 将浮动数值转换为字符串，保留两位小数

  u8g.print("cat:");
  u8g.print(weightStr);  // 显示重量值，单位是千克（kg）
   u8g.print("kg");
}

void loop() {
  u8g.firstPage();
  
   Weight = Get_Weight();  //计算放在传感器上的重物重量
  Serial.print(float(Weight/1000),3);  //串口显示重量，3意为保留三位小数
  Serial.print(" kg\n"); //显示单位
  Serial.print("\n");  //显示单位
  delay(500);  //延时2s 两秒读取一次传感器所受压力
  // 发送重量数据到上位机（通过 Serial 发送）
  sendWeightToPC(Weight);

  do {
    draw();
  } while (u8g.nextPage());

  // 读取 ESP8266 的返回信息
  while (espSerial.available()) {
    byte incomingByte = espSerial.read();
    Serial.write(incomingByte);
    
    // 判断是否接收到特定的数据包 (0x55 0x01 0x0B)
    if (incomingByte == 0x55) {
     byte secondByte = espSerial.read();  // 读取第二个字节
      byte thirdByte = espSerial.read();   // 读取第三个字节

      // 判断是否为 0x55 0x01 0x0B
      if (secondByte == 0x01 && thirdByte == 0x0B) {
        Serial.println("Received 0x55 0x01 0x0B, turning ON the servo.");
        rotateServoClockwise();
      }
      // 判断关闭信号，旋转逆时针
      else if (secondByte == 0x02 && thirdByte == 0x0B) {
        Serial.println("Received 0x55 0x00 0x0B, turning OFF the servo.");
        rotateServoCounterClockwise();
      }
    }
  }

  // 读取 Arduino 串口信息并发送到 ESP8266
  //while (Serial.available()) {
  //  espSerial.write(Serial.read());
  //}
}

// 发送重量数据到 PC
void sendWeightToPC(float weight) {
  espSerial.println("AT+CIPSEND=0,16\r");  
  delay(500);  //延时2s 两秒读取一次传感器所受压力
  // 你可以根据需求封装数据的格式
  String data = "Aight:" + String(weight / 1000, 3) + "kg\r";  // 格式化成字符串
  espSerial.println(data);  // 通过串口发送
  //espSerial.write("123456789\r");
  //espSerial.println("AT+CIPSEND=?\r");  


}

// 顺时针旋转舵机
void rotateServoClockwise() {
  myservo.attach(SG90);          //修正脉冲宽度
  myservo.write(150);
  //for (pos = 0; pos <= 180; pos += 1) {  // 顺时针旋转
  //  myservo.write(pos);
  //  delay(10);  // 每次增加1度
  //}
}

// 逆时针旋转舵机
void rotateServoCounterClockwise() {
  myservo.attach(SG90);          //修正脉冲宽度
  myservo.write(180);
  //for (pos = 180; pos >= 0; pos -= 1) {  // 逆时针旋转
  //  myservo.write(pos);
  //  delay(10);  // 每次减少1度
  //}
}


void readEspResponse() {
  while (espSerial.available()) {
    Serial.write(espSerial.read());
  }
}


//称重函数
long Get_Weight()
{
 HX711_Buffer = HX711_Read();    ///读取此时的传感器输出值
 Weight_Shiwu = HX711_Buffer;     ///将传感器的输出值储存
 Weight_Shiwu = Weight_Shiwu - Weight_Maopi; //获取实物的AD采样数值。
 Weight_Shiwu = (long)((float)Weight_Shiwu/GapValue);    //AD值转换为重量（g）
 return Weight_Shiwu; 
}
unsigned long HX711_Read(void) //选择芯片工作方式并进行数据读取
{
 unsigned long count;   ///储存输出值  
 unsigned char i;     
   ////high--高电平 1  low--低电平 0  
 digitalWrite(HX711_DT, HIGH);   ////  digitalWrite作用： DT=1；
 delayMicroseconds(1); ////延时 1微秒  
 digitalWrite(HX711_SCK, LOW);  ////  digitalWrite作用： SCK=0；
 delayMicroseconds(1);   ////延时 1微秒  
 count=0; 
  while(digitalRead(HX711_DT));    //当DT的值为1时，开始ad转换
  for(i=0;i<24;i++)   ///24个脉冲，对应读取24位数值
 { 
   digitalWrite(HX711_SCK, HIGH);  ////  digitalWrite作用： SCK=0；
                                /// 利用 SCK从0--1 ，发送一次脉冲，读取数值
  delayMicroseconds(1);  ////延时 1微秒  
  count=count<<1;    ///用于移位存储24位二进制数值
  digitalWrite(HX711_SCK, LOW);   //// digitalWrite作用： SCK=0；为下次脉冲做准备
 delayMicroseconds(1);
   if(digitalRead(HX711_DT))    ///若DT值为1，对应count输出值也为1
   count++; 
 } 
  digitalWrite(HX711_SCK, HIGH);    ///再来一次上升沿 选择工作方式  128增益
 count ^= 0x800000;   //按位异或  不同则为1   0^0=0; 1^0=1;
///对应二进制  1000 0000 0000 0000 0000 0000  作用为将最高位取反，其他位保留原值
 delayMicroseconds(1);
 digitalWrite(HX711_SCK, LOW);      /// SCK=0；     
 delayMicroseconds(1);  ////延时 1微秒  
 return(count);     ///返回传感器读取值
}
