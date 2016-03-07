# org.common.annotation 包说明

* 包含元数据注解（@MetaData）、参数解析器、参数解析器工厂

# MetaData

# 参数解析
## 概念模型
* 参数解析器 ParameterResolver<T>
	*  T：期望的参数类型
* 参数解析器工厂类 ParameterResolverFactory
* 通过一个活多个参数解析器工厂，依据输入的一组成员注解、期望参数类型、和一组参数的注解，创建对应的参数解析器。
再通过参数解析器解析出期望参数类型的参数。

## 参数解析器实现类
### DefaultParameterResolverFactory 中包含三个私有解析器实现（按优先级）
* 消息参数解析器 MessageParameterResolver （返回期望参数类型对应的消息）
* 元数据注解解析器 AnnotatedMetaDataParameterResolver （返回期望参数类型和参数注解，对应的消息的相同元数据注解的元数据）
* 元数据参数解析器 MetaDataParameterResolver （返回期望参数类型对应的消息的元数据）

### FixedValueParameterResolver	固定值参数解析器
* 用于简单资源参数解析器工厂(SimpleResourceParameterResolverFactory)，直接返回对应的资源对象为参数值

### SpringBeanParameterResolver SpringBean参数解析器
* 用于SpringBean(SpringBeanParameterResolverFactory)

## 参数解析器工厂实现类
* DefaultParameterResolverFactory
* SimpleResourceParameterResolverFactory
* SpringBeanParameterResolverFactory
* MultiParameterResolverFactory
* ClasspathParameterResolverFactory

# 定义消息处理器 HandlerDefinition<T extends AccessibleObject> 
## AbstractAnnotatedHandlerDefinition<T extends Annotation>

AbstractAnnotationHandlerBeanPostProcessor<I, T extends I>

AbstractMessageHandler
MethodMessageHandler
MethodMessageHandlerInspector

MessageHandlerInvocationException
MessageHandlerInvoker

UnsupportedHandlerException extends AxonConfigurationException
