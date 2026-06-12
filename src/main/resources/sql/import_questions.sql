-- ============================================
-- 测试题导入SQL
-- 课程: BNU-1002842007 数据库系统原理
-- 共 274 道题
-- ============================================

-- course_id 和 knowledge_point_id 后续手动关联
-- 章节信息见 analysis 字段末尾

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '一个抽象类型包括数据对象、和一组处理数据的操作。', '["A. 数据对象中各元素间的结构关系", "B. 数据元素集", "C. 接口", "D. 数据对象集"]', 'A', '【所属章节：第1讲 数据结构的基础概念】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '抽象数据类型具有 、信息隐蔽的特点。', '[]', '', '【所属章节：第1讲 数据结构的基础概念】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '线性表是具有n个（）的有限序列（n>0）', '["A. 数据对象", "B. 数据元素", "C. 字符", "D. 数据项"]', 'B', '【所属章节：第1讲 线性表的概念】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '线性表是一个（  ）。', '["A. 有限序列，可以为空", "B. 有限序列，不可以为空", "C. 无限序列，可以为空", "D. 无限序列，可以为空"]', 'A', '【所属章节：第1讲 线性表的概念】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '线性表的特点是每个元素都有一个前驱和一个后继。（）', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第1讲 线性表的概念】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '线性结构只能用顺序结构来存放，非线性结构只能用非顺序结构来存放。（ ）', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第2讲 数据结构的内容】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '1、数据结构的逻辑结构分为集合、线性、层次和 四种。', '[]', '', '【所属章节：第2讲 数据结构的内容】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '2、数据结构的存储结构分为 和非顺序两种。', '[]', '', '【所属章节：第2讲 数据结构的内容】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '3、在线性结构、树形结构和图结构中，数据元素之间分别存在着一对一、一对多和  联系。', '[]', '', '【所属章节：第2讲 数据结构的内容】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '若长度为n的线性表采用顺序存储结构，在其第i个位置插入一个新元素的算法的时间复杂度为（ ）(1<=i<=n+1)。', '["A. O(1)", "B. O(n)", "C. O(n*n)", "D. O（）"]', 'B', '【所属章节：第2讲 线性表的顺序存储】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '若长度为n的线性表采用顺序存储结构，删除第i个位置的元素，需要移动的元素个数为（  ）。', '["A. i", "B. n-i", "C. n-i+1", "D. n-i-1"]', 'B', '【所属章节：第2讲 线性表的顺序存储】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '当需要用一个形式参数直接改变对应实参的值时，该形式参数应说明为。', '["A. 与实参同类型指针参数", "B. 不需要参数", "C. 与实参同类型的参数", "D. 全局变量"]', 'A', '【所属章节：第3讲 数据结构与c语言表示】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '对一个长度为n的顺序表，假设在任何位置上插入一个元素的概率是相等的，那么插入一个元素时要移动表中的（  ）个元素。', '["A. n", "B. n+1", "C. （选项）", "D. （选项）"]', 'C', '【所属章节：第3讲 线性表顺序结构应用示例及小结】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '线性表的顺序存储是指将表中元素按照从大到小或从小到大存储。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第3讲 线性表顺序结构应用示例及小结】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '1、执行下面的程序段的时间复杂度为 。for(int i=0;i<m;i++) for(int j=0;j<n;j++) a[i][j]=i*j;', '["A. O()", "B. O()", "C. O(m*n)", "D. O (m+n)"]', 'C', '【所属章节：第4讲 算法性能评价】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '2、执行下面程序段时，语句S的执行次数为 。for(int i=0;i<=n;i++) for(int j=0;j<i;j++) S;', '["A. （选项）", "B. （选项）", "C. n(n+1)", "D. （选项）"]', 'D', '【所属章节：第4讲 算法性能评价】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '通过表达式可以获取带头结点的单链表L中首元素结点的数据值。', '["A. L->next", "B. (L->next)->data", "C. L->data", "D. L->next"]', 'B', '【所属章节：第4讲 线性表的链式存储】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '单链表中必须设有头结点。（）', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第4讲 线性表的链式存储】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '下列选项中， 项是链表不具有的特点。', '["A. 插入和删除运算不需要移动元素", "B. 所需要的存储空间与线性表的长度成正比", "C. 不必事先估计存储空间大小", "D. 可以随机访问表中的任意元素"]', 'D', '【所属章节：第5讲 单链表的基本运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '有一个带头结点的单链表HEAD，则判断其是否为空链表的表达式是', '["A. HEAD= =NULL", "B. HEAD-〉NEXT= =NULL", "C. HEAD-〉NEXT= =HEAD", "D. HEAD！=NULL"]', 'B', '【所属章节：第5讲 单链表的基本运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '在一个单链表中P所指结点后插入一个S所指结点时，应执行语句： 。', '["A. P->next=S;S->next=P->next;", "B. S->next=P->next;P->next=S;", "C. S->next=P->next;P=S;", "D. S->next=P;P->next=S;"]', 'B', '【所属章节：第5讲 单链表的基本运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '算法设计的要求是：正确性、可读性、和高效率和低存储 。', '["A. 确定性", "B. 健壮性", "C. 可行性", "D. 有限性"]', 'B', '【所属章节：第5讲 算法与算法的描述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '算法具有 有限性、确定性、      、输入、输出五大特性。', '["A. 可行性", "B. 可读性", "C. 健壮性", "D. 正确性"]', 'A', '【所属章节：第5讲 算法与算法的描述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '设指针变量p指向单链表中结点A的直接前驱，若删除单链表中结点A，则需要修改指针的操作序列为（ ）。', '["A. q=p->next；p->next=q->next；free(q)；", "B. q=p->next； p->next=q->next；", "C. p->next=p-> next->next；", "D. q=p->next；p->data=q->data；free(q)；"]', 'A', '【所属章节：第6讲 单链表运算的应用示例及小结】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '对链表进行插入和删除操作时不必移动链表中结点。( )', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第6讲 单链表运算的应用示例及小结】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '在单链表中，可以从头结点出发，查找到表中所有结点。（ ）', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第6讲 单链表运算的应用示例及小结】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '有一个带头结点的循环单链表HEAD，则判断其是否为空链表的条件是    。', '["A. HEAD==NULL", "B. HEAD-〉NEXT==NULL", "C. HEAD-〉NEXT==HEAD", "D. HEAD！=NULL"]', 'C', '【所属章节：第7讲 循环链表】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '在单向循环链表中，从表中任意结点出发都可以顺着next域访问到表中所有元素（）', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第7讲 循环链表】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '与单链表相比，双向链表的优点之一是 。', '["A. 插入删除操作更加方便", "B. 可以进行随机访问", "C. 可以省略表头指针和表尾指针", "D. 访问前后相邻结点更方便。"]', 'D', '【所属章节：第8讲 双向链表】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '在双向链表L中，可以从任一结点p出发沿同一方向的指针域查找到表中所有元素。（）', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第8讲 双向链表】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '静态链表中与动态链表的插入和删除运算类似，不需要做元素的移动。（）', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第9讲 静态链表】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '静态链表既有顺序存储结构的优点，又有动态链表的优点。所以，它存取表中第ｉ个元素的时间与位置序号ｉ无关，可以实现随机存取。（）', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第9讲 静态链表】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SQL字面含义是“查询语言”，但其功能包括数据定义、查询、修改和保护等许多内容。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 SQL概述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系数据库的标准语言是SQL,是Structured Query Language的简称，意思是是结构化查询语言。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 SQL概述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '基本Select语句的一般形式共有七个子句，都是可选的。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 SQL概述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SQL语言是大小写不敏感的。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 SQL概述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '查询（SELECT）语句中其它子句都可以不出现，但至少要有一个SELECT子句。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 SQL概述】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '关系数据库系统的术语中行是（ ）。', '["A. 元组", "B. 关系", "C. 属性", "D. 域"]', 'A', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '（ ）能够唯一地标识表中的一行数据。', '["A. 主键", "B. 超键", "C. 候选键", "D. 以上都是"]', 'D', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '一个表只能有一个（ ）。', '["A. 主键", "B. 候选键", "C. 替换键", "D. 以上都是"]', 'A', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '对于关系中要求其值必须与其他关系中的主键匹配的属性或属性组，称为（ ）。', '["A. 候选键", "B. 主键", "C. 外键", "D. 匹配键"]', 'C', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '关系数据库系统的术语中列是（ ）。', '["A. 元组", "B. 关系", "C. 属性", "D. 记录"]', 'C', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '关系数据库系统的术语中表是（ ）。', '["A. 元组", "B. 关系", "C. 属性", "D. 域"]', 'B', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '属性可以具有的值的合法集合是（ ）。', '["A. 元组", "B. 关系", "C. 属性", "D. 域"]', 'D', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系数据库使用一个或多个表来存储数据。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系数据库中，同一表中行次序无关紧要。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系数据库中，因为每个列都有一个名字，同一表中列次序无关紧要。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 关系结构和约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个关系模式中不同属性在取值上总会存在相互依赖又相互制约，这种属性与属性之间的联系，称为函数依赖。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 函数依赖】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '给定关系模式S的函数依赖集D， D逻辑蕴涵的所有函数依赖的集合称为D的闭包。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 函数依赖】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '考官表中，按照反射律，(erid,ername)→ername成立。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 函数依赖】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '考官表中，erid→ername成立，则(erid,erage)→(ername,erage)也成立，依据是增广律。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 函数依赖】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当考官号→考官院系名，考官院系名→考官院系办公地点，都成立时，考官号→考官院系办公地点 也成立，这依据的是增广律。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 函数依赖】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'H与G等价则H中的每个函数依赖属于H的闭包。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 函数依赖】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '典型大数据应用中的数据在如下的一个或多个（4V）方面与传统技术面对的数据表现出显著不同：数据量（Volume）大、类型（Variety）多样、速度（Velocity）快、价值（Value）高而密度稀疏。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 大数据及其特征】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '大数据技术的目标乃是简单、高效并安全地共享大数据，支持大数据应用。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 大数据及其特征】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '大数据技术的关键需求包括：①可伸缩性，能够有效处理越来越多的数据和越来越多的访问。②可靠性，能够容忍实际合理的故障。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 大数据及其特征】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据库系统的特点之一就是由数据库管理系统提供统一的机制保护数据安全。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据保护】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据完整性指对数据的期望使用能力，保护数据可用性通常指减少数据库系统停工时间，保持数据持续可访问。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据保护】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据完整性包括数据值的完整性和数据来源的完整性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据保护】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据库管理系统中的故障恢复机制不仅维护故障情况下的数据完整性，并且由于故障恢复机制对故障的有效处理，它也是保护数据可用性的重要手段。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据保护】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '定义了完整性约束，就可以保证数据始终真实正确。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据保护】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '下列软件系统中，（ ）不属于数据库管理系统。', '["A. PostgreSQL", "B. Oracle", "C. MySQL", "D. excel"]', 'D', '【所属章节：第一节 数据库】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '只要有大量数据就可以称之为数据库。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据库】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据库管理系统DBMS有多种，比如PostgreSQL、Oracle等。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'B/S结构的软件需要针对不同的操作系统开发不同版本的软件，每台客户机需要安装专门的客户端，而且当系统升级时，每一台客户机都需要重新安装客户端新版本。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据库应用体系结构】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SQL语言和C/C++/Java/C#等高级语言混合编程时，可以将静态或动态SQL语句嵌入高级语言，也可以让高级语言通过ODBC、JDBC、ADO等调用SQL。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库应用体系结构】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'C/S系统利用SQL语言和C/C++/Java/C#等高级语言的各自优势，SQL语言访问数据库，C/C++/Java/C#等高级语言进行数据处理和表示。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库应用体系结构】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'C/S结构通常包括多层：浏览器层、Web服务器、应用服务器、数据库服务器和数据库。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据库应用体系结构】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '从HTML到SQL需要两个桥梁，HTML与高级语言之间的JDBC、ODBC、ADO等桥梁；高级语言与数据库之间的CGI、ASP、JSP等桥梁。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据库应用体系结构】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据库设计方法主要包括实体-联系方法和属性-联系方法两种。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '实体-联系设计方法围绕实体展开，从软件工程角度来看，数据库生命周期经历需求分析、概念设计、逻辑设计、物理设计、数据库实现以及运行维护等阶段。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '概念模式与具体DBMS有关。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '概念模式通常使用实体-联系图表示。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '属性-联系数据库设计方法围绕属性展开。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '属性-联系设计方法，是在需求分析的基础上直接采用属性-联系方法进行逻辑设计，也就是把数据库保存的所有属性放在一张关系表中，进而通过属性之间的联系，来不断的优化这个关系表的模式，得到期望的结果模式。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '实体联系方法，以实体为中心，着重于一个关系模式，基本对应一个实体或联系，即关系模式与实体或联系之间基本是一一对应的。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '属性联系方法，以属性为中心，着重于属性之间的依赖关系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '实际当中的数据库设计，通常将两种方法相结合，宏观上采用实体-联系方法，微观上采用属性-联系方法，也就是对由概念模式转换而来的关系表运用属性-联系方法进行分析优化。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第一节 数据库设计方法和生命周期】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据被加密后的结果称为密文，把密文还原为明文的过程称为解密。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第七节 加密】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '加密体系中最核心的是用于加密解密的算法和密钥。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第七节 加密】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '现代加密体系中算法通常是公开的，密钥是保密的并且需要向可信权威机构申请，安全性完全取决于密钥的保密性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第七节 加密】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'JDBC是一种用于执行SQL语句的Java API，它定义了用来访问PG数据库的标准Java类库，只能是PG的SQL语句，而不包括其它关系数据库系统。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'JDBC定义了用来访问数据库的标准Java类库，使用这个类库可以以一种标准的方法、方便地访问各种关系数据库，包括pgSQL。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'Java通过JDBC来访问数据库，这些包含了JDBC连接访问数据库语句的Java程序就是普通Java程序，需要导入相应的包，即import Java.sql.*，用Java语言编译器编译成字节码，就可以在Java虚拟机上运行了。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '用 Class.forName (“org.postgresql.Driver”);加载JDBC驱动程序后就可以访问各种关系数据库系统，比如ORACLE等。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'JDBC还提供了查询结果集模式和数据库模式的机制。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'JSP是一种使用Java开发Web应用程序的服务器端脚本技术，它把Java代码嵌入在html文档中，用<% 和 %>括起来的部分是html代码。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '用JSP编写的动态Web页面中，会出现用<% 和 %>括起来的Java代码，包括通过JDBC访问数据库的加载驱动程序、建立数据库连接、创建Statement对象，执行pgSQL语句以及善后处理，以及输出等。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 JDBC编程】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当把一个实体转换为一个关系模式时，实体的一个属性对应为该表的一个列，实体的主键就是表的主键。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 基本E-R图转换为关系模式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个联系转换为一个关系模式，联系的属性对应表的属性，并上所有参与联系的各实体主键的并集。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 基本E-R图转换为关系模式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '由联系转换来的表的主键﻿与任一端实体主键相同。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 基本E-R图转换为关系模式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个1：1联系转换的表可与任一端实体对应的表合并在一起。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 基本E-R图转换为关系模式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个1：n联系转换的表可以与1端对应的表合并在一起。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 基本E-R图转换为关系模式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '大数据计算有多种计算模式，最常见的是批处理和流计算两种。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '流式计算处理的源数据通常是开放的，都是流数据，也称流式数据，是指将数据看作数据流的形式来处理。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '批处理的源数据通常是封闭的，通常将需要处理的大批量数据存入硬盘，处理的时候再从硬盘中读取数据进行一次性处理，如果产生了中间结果，需将中间结果写入外存，再继续后面的处理，因此批处理的I/O操作相对更加频繁。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'Storm流式计算框架结构中包括Spout和Bolt两种组件。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'Bolt用于从外部数据源接收数据，然后将其喷发到拓扑中的相应组件中去。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'Storm集群中守护进程Supervisor运行在主结点上，负责代码分发，为工作结点分配任务故障监测；守护进程Nimbus运行在工作结点上，负责监听分配给所在工作结点的任务，即根据Nimbus的任务分配来决定启动或停止工作进程执行Storm拓扑，一个Supervisor可能执行拓扑的一部分，也可能执行完整的拓扑。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'Storm集群中Supervisor保存了Storm的状态信息。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'MapReduce中，Map任务接收从不同Reduce任务得到的键相同的键-值对，执行用户编写的Map函数，将键相同的键-值对中的所有值以Map函数指定的方式组合起来，得到键-值对并输出。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 大数据计算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '数据库管理系统的目标是( )地共享数据。', '["A. 简单", "B. 高效", "C. 安全", "D. 所有其余三个选项"]', 'D', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '数据模型是数据结构和语义的概括，比如有（ ）等等。', '["A. 层次模型", "B. 关系模型", "C. 实体-联系模型，也叫E-R模型", "D. 其它所有三个选项都对"]', 'D', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '应用程序员一般按照（ ）模式访问数据库中的数据。', '["A. 内", "B. 外", "C. 逻辑", "D. 物理"]', 'B', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '数据库管理系统提供访问数据库的语言一般包括（ ）。', '["A. 数据定义语言", "B. 数据操作语言", "C. 数据保护语言", "D. 所有其它三个选项"]', 'D', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '数据独立性指的是（  ）。', '["A. 数据与数据所有者相互独立", "B. 数据的组织存储结构与应用程序独立", "C. 数据和数据之间彼此孤立", "D. 数据与磁盘之间相互独立"]', 'B', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '三层模式结构中最接近外部存储器的是（ ）。', '["A. 模式", "B. 外模式", "C. 内模式", "D. 概念模式"]', 'C', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '为特定用户定义一个或多个数据库视图的模式是（ ）。', '["A. 外模式", "B. 内模式", "C. 概念模式", "D. 以上都不对"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当模式改变时，对各外模式／模式映射作相应的改变，应用程序无需修改，这是数据逻辑独立性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果数据库的内模式变化，只要对模式／内模式映射作相应的修改，模式可以保持不变，这是数据物理独立性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据字典中存放元数据，比如数据模式、外模式等。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据索引帮助快速定位特定数据项。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '日志登记对数据的修改。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '保护管理模块以一种称为“事务”的方式，维护多用户并发访问及故障情况下的数据一致性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '数据抽象中，（ ）层从某个或某类用户角度出发，只描述与其相关的那部分数据。', '[]', '', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '数据抽象中，（ ）层描述数据实际上是怎样在辅助存储设备上组织的。', '[]', '', '【所属章节：第三节 数据库管理系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '投影是指选取表中的某些列的列值；广义投影是指在选取属性列时，允许进行适当运算。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果SELECT后面是最简单的形式即单独一个×，这种情况输出FROM子句给出表中的所有列值。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'ORDER BY子句让查询结果中的行按一个或多个列或列表达式的值进行排序，升序时用ASC，降序时用DESC，默认为升序。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '升序时排序列为空值的行最后显示，降序时排序列为空值的行最先显示。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '除非在SELECT后跟DISTINCT明确指出要求去重，否则，默认情况下、或者SELECT后跟ALL时都保留重复。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '选择操作用WHERE子句实现，从表中选择满足给定条件的行。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PostgreSQL支持聚集查询，允许从多个输入行中计算出一个结果。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SUM和AVG的输入必须是数值型的。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '聚集函数可以进行复合运算。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'GROUP BY子句利用指定列进行分组，所有给出列上取值相同的行被分在一个组。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '可以使用GROUP BY子句将聚集函数作用在组上。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '使用HAVING子句可以对GROUP BY子句形成的分组进行筛选。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'HAVING子句给出的条件只针对GROUP BY子句形成的分组起作用，也可以使用聚集函数。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '默认情况下SELECT的执行会自动去重。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SUM和AVG可作用在非数值数据类型的列上。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 简单查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '设K为S< A , D >的超键，若K完全决定A，则称K为S的候选键。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 范式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果关系模式S∈1NF，且每一个属性都不部分依赖于S的任何候选键，则S∈2NF。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 范式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果关系模式S<A，D>是1NF，且每个属性都既不部分也不传递依赖于S的任何候选键，那么称S是第三范式(3NF)的模式。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 范式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果关系模式S<A，D>是第三范式，它的任何一个主属性都既不部分也不传递依赖于S的任何候选键，则称S∈BCNF。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 范式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系模式S<A，D> ，它的任何一个主属性都既不部分也不传递依赖于任何候选键，则称S∈BCNF。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 范式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系模式S<A，D>∈1NF，其D中任意一个非平凡函数依赖的决定因素都包含键，则S∈BCNF。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 范式】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG使用角色来统一管理用户。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 访问控制】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG提供GRANT 语句来给角色撤销数据库操作权限。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 访问控制】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', 'CREATE ROLE nini SUPERUSER;该语句的功能是（）。', '[]', '', '【所属章节：第三节 访问控制】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '定义附加运算没有增加关系代数的表达能力。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 附加关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '集合交运算必须在相容的关系间进行。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 附加关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '自然联接运算的计算过程是：首先计算笛卡尔积；然后在笛卡尔积的结果上，基于两个参数的关系模式中都出现的属性，即两个关系模式的所有同名属性进行属性值相等的选择运算；最后去除重复列。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第三节 附加关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '属性联接是在笛卡尔积的基础上选取满足给定条件的元组。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第三节 附加关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '关系代数运算有（ )。', '["A. 附加关系代数运算", "B. 扩展关系代数运算", "C. 基本关系代数运算", "D. 以上都对"]', 'D', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '下列哪个运算不属于基本关系代数运算？', '["A. 投影", "B. 平方", "C. 交", "D. 选择"]', 'C', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '选择运算是选出满足给定谓词(条件)的元组，结果关系和原关系有着相同的模式。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '投影运算用来从给定关系产生一个只有其部分列的新关系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系代数每个运算都是去重的。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '对于关系并运算，参与运算的关系必须是相容的。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '对于关系的笛卡尔积运算，结果关系的模式是参与运算的两个关系的模式的串接。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '对于关系的笛卡尔积运算，运算符左侧关系中的每一个元组与右侧关系的每一个元组拼接，形成结果关系中的一个元组。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系运算的运算参数是关系，运算结果也是关系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '今有关系X和Y结构相同，且各有10个元组，那么这两个关系的交运算结果的元组个数是10。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '今有关系X和Y结构相同，且各有10个元组，那么这两个关系的自然联接运算结果的元组个数是10。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 基本关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '与早前水平扩展的思想不同，目前面对大数据挑战，总是采用垂直扩展的方式。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当前大数据应用中的分布式文件系统通常都采用主从结构。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'Google GFS中每个块的大小为64MB。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '为了应对可能随时发生的故障，文件的每个块都存有不同节点上的多个副本。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当前大数据应用中的分布式文件系统，当面对数据修改时，需要分布式并发控制、提交和恢复机制来维护多个副本间的一致性，归根结底需要一个异步系统中的分布式共识协议。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '典型的NoSQL系统可以分为四类：键值存储系统、列族存储系统、文档存储系统和图存储系统。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '图数据库使用图作为数据模型，有一个节点集合和表征了节点关系的边集合。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 大数据存储技术】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG提供的SQL语言可以作为子语言嵌入在宿主语言中使用，这里所说的宿主语言就是指SQL语言。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '预处理程序ECPG对.c文件进行预处理，主要就是把其中的pgSQL语句转换成主语言能够识别的SQL函数调用的形式，结果是同名.pgc文件。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '把pgSQL嵌入到宿主语言中使用还必须要解决以下四个方面的问题：连接数据库、嵌入识别问题、宿主语言与pgSQL语言的数据交互问题、宿主语言的单记录与pgSQL的多元组的协调问题。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '宿主语言连接数据库，通常需要给出数据库服务器地址、端口号、数据库名、用户名、口令等，必要时可能还需要安装和加载数据库驱动程序。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '为了区分宿主语言和pgSQL语句，为pgSQL语句加一个识别前缀标识“EXEC SQL” ，和结束标志：分号“;”。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '解决宿主语言和pgSQL语言的数据交换问题的答案是，引入共享变量的概念。在pgSQL语句中使用共享变量时，变量名前需加一个引号。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '解决宿主语言一次只能处理一个记录，而pgSQL语言一次处理多个元组的矛盾，方法是使用共享变量。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当无法确定SELECT语句查询结果至多是一个元组时，需要用游标机制把多个元组一次一个地传送给宿主语言程序进行处理。另外，在游标处于活动状态时，也可以更新或删除游标指向的元组。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SQLSTATE是一个特殊变量，用于连接pgSQL执行系统和宿主语言，它是一个五字符的数组，每次调用pgSQL的库函数，向SQLSTATE变量中存放一个代码，以反应调用中出现的问题，比如〞02000〞表示没有产生任何错误，〞00000〞表示没有找到结果元组。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果实际应用中有时SQL语句只能在实际运行时才能完全确定，这时就需要动态pgSQL语句。动态pgSQL语句需要先准备再执行。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 嵌入式pgSQL】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '定义表的属性时不必指明数据类型。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 数据定义与修改】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG中使用单引号做字符串常量的标识，对于包含单引号的字符串，直接使用双引号。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 数据定义与修改】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '在插入的常量元组中不能出现空值null。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 数据定义与修改】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG中使用单引号做字符串常量的标识，任何字符串中不能包含单引号。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 数据定义与修改】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '通过Web界面访问在线服务时，动态页面一般都是使用数据库中的数据生成。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 数据库系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '通过浏览器访问在线服务，就是使用数据库系统的一个例子。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 数据库系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '通过Web界面访问在线服务，是由html页面直接从数据库中获取数据并展示给用户，不需要任何应用程序设计语言的帮助。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 数据库系统】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '模式分解需要关注的特性是：是否无损联接、是否保持依赖。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 模式分解】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '把一个关系模式分解为两个关系模式时，分解具有无损联接性的充分必要条件是两个关系模式的公共属性是其中一个模式的键。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 模式分解】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果某个分解不能保持函数依赖，则分解后的模式利用函数依赖约束来保护数据完整性的能力将会被削弱。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 模式分解】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '非无损联接的分解意味着分解将导致信息丢失。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 模式分解】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '保持函数依赖的分解必定具有无损连接性。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 模式分解】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'E-R图主要包括实体和联系以及它们各自的属性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '像这样能够并且用来区分一个实体集中不同的一个个实体的最小的属性集或者是属性组（一组属性），称为实体标识符，（简称标识符）也称为实体主键（简称主键）。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '联系不可能有属性', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '联系关联的实体个数称为该联系的元数或度数。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '同类实体集内部实体与实体之间的联系，称为一元联系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '两个不同实体集中实体之间的联系，称为二元联系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果实体集E1中，每个实体可以与实体集E2中任意个（零个或多个）实体之间具有联系，并且E2中每个实体至多和E1中一个实体有联系，那么我们就把E1对E2的联系称为“一对多联系”。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第一小节 E-R模型元素】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个人会喜欢另一个人，人与人之间的联系“喜欢”是一个一元联系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第二小节 基本E-R图设计】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个辅导员只辅导一个班级，一个班级只由一个辅导员来管理，辅导员和班级之间的联系，是一个一对一的联系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 第二小节 基本E-R图设计】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '视图可以对数据存在性方面的保密性提供保护。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 视图】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '可以使用视图定义视图。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 视图】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据库只存储表的定义，不存储相应的数据。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第二节 视图】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '系统只存储视图的定义，不存视图的数据。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 视图】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '视图支持实现数据的逻辑独立性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第二节 视图】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '视频中的图书馆数据库设计，只有第四种方案是正确的，完美的，任何情况下都只能采取这种方案。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第五节 大数据E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '同一个数据库，设计达到的范式越高，结果表个数越多。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 大数据与反规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '若关系模式的规范化程度越高，优势在于数据冗余、插入异常、删除异常、修改复杂等问题越少。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 大数据与反规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '较低范式的劣势在于数据冗余造成的空间代价以及修改代价（插入异常、删除异常、修改复杂）高。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 大数据与反规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '数据冗余毫无益处。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第五节 大数据与反规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系模式满足的范式级别越高越好。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第五节 大数据与反规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一个SELECT-FROM-WHERE语句称为一个查询块，将一个查询块嵌套在另一个查询块的SELECT、FROM、WHERE、GROUP BY、HAVING、ORDER BY、LIMIT、OFFSET或WITH子句中的查询称为嵌套查询。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 嵌套查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '由于SELECT语句的结果就是一个表，所以查询块可以出现在另外一个查询中表名可以出现的任何地方,主要有FROM子句和WITH子句。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 嵌套查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '查询块也可以出现在集合能够出现的任何合适的地方。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 嵌套查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '在写嵌套查询语句时，如果能确定查询块只返回单行单列的单个值，查询块可以出现在单个属性名、单个表达式、单个常量，即单值表达式能够出现的任何地方。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 嵌套查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '触发器比CHECK约束更灵活，可以实施各种复杂的检查和操作，具有更精细和更强大的数据保护能力。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '触发器可以定义在触发事件之前或之后，即分为BEFORE和AFTER触发器，分别在操作完成前和操作完成后触发，执行触发器函数。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '行级触发器的触发器函数为每条触发语句执行一次。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '触发器函数必须返回一个NULL或者一个元组类型的变量。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '行级after触发器的值总是被忽略，可以返回null；行级before触发器的返回值不同，对触发器操作的影响也不同，如果返回NULL则忽略该触发器的行级别操作，其后的触发器也不会被执行，如果非NULL则返回的行将成为被插入或者更新的行。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '通常，用行级before触发器检查或修改将要插入或者更新的数据。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '语句级before触发器在触发语句开始之前触发。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '行级after触发器在修改由触发SQL语句影响的每一行记录之后触发。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第五节 触发器】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '事务具有原子性一致性、隔离性、持久性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '事务持久性指应用程序所定义的事务，其单独成功的执行，必定是使数据库从一个一致性状态变到另一个一致性状态。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '事务原子性指一个事务一旦被提交，它对数据库中数据的改变就应该是持久性的，接下来的其它操作或故障不应该对其执行结果有任何影响。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG用BEGIN和COMMIT（或ROLLBACK）将数据库访问操作指令序列包围以声明一个事务。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG提供三种独立的事务隔离级别，分别是读已提交READ COMMITTED、REPEATABLE READ和可串行化SERIALIZABLE。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '可串行化事务中的语句看到的是该事务开始时的快照。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '可串行化级别提供最严格的事务隔离，因为事务是一个接着一个串行执行的。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第六节 事务】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'PG允许使用各种不同的程序设计语言来编写函数，特别是内建了PL/pgSQL。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 PG中的函数】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '存储函数的主要优势之一是运行效率高。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 PG中的函数】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '推荐系统通过研究用户的兴趣爱好，帮助用户从大数据中发觉自己潜在的需求，进行个性化推荐，缓解或解决信息过载问题。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 大数据应用】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '基于内容推荐方法根据用户兴趣模型与每一个物项特征模型之间的相似性来进行推荐。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 大数据应用】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '基于用户的协同过滤推荐方法是根据用户相似性进行推荐。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 大数据应用】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '基于物项的协同过滤推荐方法是根据物项相似性推荐。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 大数据应用】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '主键约束意味着各元组主键值不能重复，且不能为空。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'eeexam表的主键由eeid，eid属性组成，此时主键由两个属性一起组成，可以同时在eeid、eid属性声明的后面分别写上PRIMARY KEY就可以了。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '将一个表的一个或几个属性定义为候选键后，插入元组或对主键列进行修改操作时，系统自动检查主键的各个属性是否为空，只要有一个为空就拒绝插入或修改；并且检查候选键值是否唯一，如果不唯一则拒绝插入或修改。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '删除操作不会检验主键约束。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '将一个表的一个或几个属性定义为外键后，被引用表删除或修改时，系统不会自动检查是否将会违背外键约束。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '非空约束意味着每个元组对应列值不能为空。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '每当插入元组或者修改表中元组时, 就对基于元组的CHECK约束中的条件进行检验。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 完整性约束】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '所有属性都不足以形成主键的实体称为弱实体。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '其属性可形成主键的实体集称为弱实体集。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '含弱实体E-R图向关系模式转换时，一个关联弱实体的联系和弱实体一起转换为一个关系模式，主键是参与联系的强实体的主键。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '根据实体间的区别在实体集内部进行分组的过程称为特殊化。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一般化从单一的实体集出发，通过创建不同的低层实体集来强调同一实体集中不同实体间的差异。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '低层实体集继承参与其高层实体集所参与的那些联系。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '低层实体集所特有的联系也适用于其高层实体集。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '用表表示父子实体集时，只需为每个低层实体集创建表。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '一般化是一种自底向上的方法。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 扩展E-R图及其转换】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '聚集运算使得关系代数表达式可以在结果中保留悬浮元组。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 扩展关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '分组聚集就是对关系中的元组按某一条件进行分组，并对每个分组使用聚集函数。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 扩展关系代数运算】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'single', '下列__________是数据库中数据的特征。', '["A. 独立性", "B. 安全性", "C. 共享性", "D. 以上所有"]', 'D', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '下列__________是使用数据库系统的优点。', '["A. 增强安全性", "B. 效率的提高", "C. 因复杂而难度加大", "D. 无法持久保存数据"]', 'B', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系数据库管理系统使用非常简单的关系模型，使得数据库设计和访问都像面对的是日常生活中广泛使用的最简单形式的表格。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'SQL语言非常接近自然语言，易学易用。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '采用三层模式两级映射获得了良好数据独立性，使得物理模式的调整和模式的调整都独立于应用程序。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '视图仅允许用户见之所需。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '允许事务并发执行虽能带来性能上的好处，但需要对并发进行管控以保证数据完整性。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '恢复机制保证并发情况下的数据完整性。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '并发控制机制能保障故障情况下的数据完整性。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '这次大数据技术浪潮涉及基础架构的变化。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '随着大数据现象的出现，数据管理技术正面临基础架构变化带来的深刻变革。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'NoSQL系统利用计算机集群这种新架构来存储和处理大数据。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '当前的NoSQL系统强调可扩展性和高性能。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '尽管出现了大数据技术，关系数据库管理系统在在线事务处理市场的主导地位稳如泰山。', '["A. 正确", "B. 错误"]', 'B', '【所属章节：第四节 数据管理技术发展趋势】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果查询的数据涉及两个或多个表，可以使用联接操作，称为联接查询。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '联接查询，涉及联接条件和联接类型两个方面。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '联接条件决定了两个表中哪些行是匹配的，以及联接结果中出现哪些列。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '联接类型是按照对悬浮行的不同处理方式来分的，分为内联接和（左/右/全）外联接。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '自然联接，即自然内联接，是在笛卡尔积的基础上选取所有同名列上取值相等的行，结果表中同名列只出现一次。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '属性联接，即属性内联接，是在笛卡尔积的基础上选取指定同名属性上取值相等的行，结果表中这些指定同名属性只出现一次。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果属性联接指定全部同名列来匹配则等价于自然联接。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '条件联接，即条件内联接，是在笛卡尔积运算的基础上选取满足给定条件的行。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '内联接抛弃所有悬浮行。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', 'pgSQL中内联接用INNER而外联接用OUTER，默认为INNER；LEFT、RIGHT、FULL均隐含外联接。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 联接查询】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '如果要求分解既具有无损联接性，又具有保持依赖性，则一定能够达到3NF，但不一定能够达到BCNF。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '把较低范式的关系模式分解为若干较高范式的关系模式的方法不是唯一的，只有能够保证分解后的关系模式与原关系模式等价，分解才有意义。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 规范化】');

INSERT INTO question (course_id, knowledge_point_id, type, content, options, answer, analysis) VALUES (4, NULL, 'judge', '关系模式规范化实际上就是一个模式分解过程：把逻辑上相对独立的信息放在独立的关系模式中。', '["A. 正确", "B. 错误"]', 'A', '【所属章节：第四节 规范化】');
