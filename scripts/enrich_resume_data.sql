-- 丰富简历数据：为学生 7~10 补充完善的简历内容
-- 根据学生各自的求职意向（前端/Python/嵌入式/硬件测试）填充真实感数据

-- ===== 学生7：前端开发工程师 =====
UPDATE resume SET
    internship = '[{"company":"阿里巴巴","position":"前端开发实习生","duration":"2023.06-2023.12","description":"参与淘宝商家后台管理系统前端开发，基于 React + TypeScript 技术栈完成 5 个核心业务模块的开发与重构。负责组件库的封装与性能优化，通过代码分割和懒加载将首屏加载时间降低 35%。与后端工程师协作完成接口联调，撰写前端技术文档。"}]',
    skills = 'Vue.js, React, TypeScript, Webpack, Axios, Element Plus, SCSS, Git, ECharts, Vite',
    self_evaluation = '对前端开发充满热情，具备扎实的 JavaScript/TypeScript 基础和 React/Vue 双框架开发经验。实习期间独立负责多个业务模块，有良好的组件化思维和性能优化意识。善于通过技术手段提升用户体验，乐于学习新技术并与团队分享。目标明确，致力于成为全栈方向的前端工程师。',
    job_target = '前端开发工程师',
    ai_analysis = '{"overallScore":72,"strengths":["有知名互联网公司实习经历","React/Vue 双框架经验丰富","有性能优化实战经验，能量化成果"],"weaknesses":["学历背景较弱，非 985/211","缺少独立项目/GitHub 开源贡献","自我评价偏泛化，缺乏差异化亮点"],"suggestions":["补充个人开源项目或 GitHub 链接，展示代码能力","在实习描述中补充技术难点攻坚细节","增加对前端工程化、CI/CD等方面的了解"],"missingFields":["项目经历（除实习外）","个人作品或博客链接","证书/竞赛奖项"],"recommendedSkills":["Node.js（全栈方向）","Docker/Nginx（部署方向）","Jest/Vitest（测试方向）"]}'
WHERE student_id = 7;

-- ===== 学生8：Python开发工程师 =====
UPDATE resume SET
    internship = '[{"company":"字节跳动","position":"Python 开发实习生","duration":"2023.07-2023.12","description":"参与公司内部数据分析平台的开发与维护，使用 Django + Pandas 实现数据处理流水线，日均处理 50 万+ 条业务数据。设计并实现自动化报表生成模块，将报表产出效率提升 60%。使用 NumPy 进行数据清洗与特征工程，参与 A/B 测试平台的数据埋点与分析。"}]',
    skills = 'Python, Django, Pandas, NumPy, MySQL, Redis, Flask, Docker, Git, Linux, RabbitMQ, RESTful API',
    self_evaluation = '对数据分析和后端开发有浓厚兴趣，具备扎实的 Python 编程基础和主流框架实战经验。实习期间独立负责数据处理模块，熟悉大数据量下的性能优化技巧。具备良好的数据思维和问题分析能力，能从业务角度出发设计技术方案。持续关注机器学习领域动态，积极拓展技术边界。',
    job_target = 'Python开发工程师',
    ai_analysis = '{"overallScore":75,"strengths":["有大厂实习经历，数据量大有说服力","技术栈完整，覆盖 Web 开发和数据处理","有明确的性能优化成果和数据支撑"],"weaknesses":["学历背景较弱","缺少机器学习/深度学习项目经验","简历内容偏简洁，细节展示不足"],"suggestions":["补充数据处理的具体技术难点和解决方案","增加个人数据分析和可视化项目","补充数据库设计或架构方面的经验"],"missingFields":["项目经历（除实习外）","证书/荣誉","个人 GitHub"],"recommendedSkills":["Spark/Flink（大数据方向）","FastAPI（高性能 API）","TensorFlow/PyTorch（AI 方向）"]}'
WHERE student_id = 8;

-- ===== 学生9：嵌入式软件工程师 =====
UPDATE resume SET
    education = '[{"school":"XX职业学院","major":"软件工程","degree":"本科","start":"2020.09","end":"2024.06","gpa":"3.5/4.0"}]',
    internship = '[{"company":"广州致远电子","position":"嵌入式软件开发实习生","duration":"2023.07-2023.12","description":"参与公司嵌入式控制模块的驱动开发与调试，基于 STM32 平台完成 I2C/SPI/UART 等外设驱动的编写与测试。使用 C 语言实现 Modbus 通信协议栈，通过逻辑分析仪和示波器排查硬件通信故障。参与 FreeRTOS 实时操作系统的任务调度优化，将系统响应延迟降低 20%。协助硬件工程师完成 PCB 板级调试。"}]',
    skills = 'C, C++, Linux, ARM, STM32, FreeRTOS, I2C, SPI, UART, Modbus, Git, Keil, 逻辑分析仪',
    self_evaluation = '嵌入式系统爱好者，动手能力强，具备从底层驱动到上层应用的全栈嵌入式开发能力。在校期间多次参与电子设计竞赛，积累了丰富的硬件调试经验。熟悉 ARM Cortex-M 系列架构和实时操作系统原理，能独立完成嵌入式项目的方案设计、编码和调试。善于阅读芯片手册和硬件原理图，注重代码的健壮性和可移植性。',
    job_target = '嵌入式软件工程师',
    ai_analysis = '{"overallScore":73,"strengths":["技能栈覆盖嵌入式开发全链路","有实际驱动开发和 RTOS 优化经验","实习有明确的量化成果（延迟降低 20%）"],"weaknesses":["实习经历不够知名企业","缺少通信协议栈的深入经验","简历中未体现竞赛奖项细节"],"suggestions":["补充学科竞赛或创新项目经历","增加对网络协议或无线通信的了解","展示独立完成的嵌入式项目，如智能小车等"],"missingFields":["项目经历","证书/竞赛奖项","个人作品链接"],"recommendedSkills":["Linux 驱动开发","RT-Thread/Zephyr（国产 RTOS）","Python（自动化测试脚本）"]}'
WHERE student_id = 9;

-- ===== 学生10：硬件测试工程师 =====
UPDATE resume SET
    education = '[{"school":"XX职业学院","major":"电子信息工程","degree":"本科","start":"2020.09","end":"2024.06","gpa":"3.6/4.0"}]',
    internship = '[{"company":"华为技术有限公司","position":"硬件测试实习生","duration":"2023.06-2023.12","description":"参与 5G 基站电源模块的硬件测试工作，根据测试用例完成功能测试、信号完整性测试和环境可靠性测试。使用示波器、万用表、频谱仪等仪器完成 80+ 项测试指标并输出测试报告。编写自动化测试脚本（Python）将回归测试效率提升 40%。参与测试用例评审，发现并提交 15+ 个硬件设计缺陷。"}]',
    skills = '硬件测试, 示波器, 万用表, 频谱仪, 信号发生器, 焊接技术, 电路分析, 测试用例设计, Python, 测试报告, Altium Designer',
    self_evaluation = '细心严谨，对硬件测试有独到见解。具备扎实的电子电路理论基础和丰富的仪器操作经验，能独立完成从测试方案设计到报告输出的全流程。实习期间参与多家供应商的硬件测试项目，积累了通信电源、消费电子等多领域的测试经验。善于通过自动化手段提升测试效率，注重测试覆盖率和问题闭环。',
    job_target = '硬件测试工程师',
    ai_analysis = '{"overallScore":76,"strengths":["有大厂硬件测试实习经历，项目含金量高","测试技能全面，覆盖功能/信号/环境多维度","有自动化测试能力（Python 脚本）"],"weaknesses":["学历背景较弱","缺少 PCB 设计或硬件开发经验","测试成果数据不够具体"],"suggestions":["补充电路设计或仿真项目经历","增加对硬件测试行业标准的了解（如 IPC/GB 标准）","展示测试发现缺陷的具体案例"],"missingFields":["项目经历","证书（如电子设计竞赛）","专业课程成绩"],"recommendedSkills":["LabVIEW（自动化测试平台）","Cadence/PADS（PCB 设计）","C++（测试工具开发）"]}'
WHERE student_id = 10;
