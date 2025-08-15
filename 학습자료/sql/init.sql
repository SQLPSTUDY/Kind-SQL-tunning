-- create titles table
CREATE TABLE titles (
    title_id VARCHAR2(50) NOT NULL,
    title VARCHAR2(255) NOT NULL,
    CONSTRAINT pk_titles PRIMARY KEY (title_id)
);

-- create departments table
CREATE TABLE departments (
    dept_no VARCHAR2(10) NOT NULL,
    dept_name VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (dept_no)
);

-- create employees table (titles를 참조하므로 먼저 생성)
CREATE TABLE employees (
    emp_no NUMBER NOT NULL,
    emp_title_id VARCHAR2(50) NOT NULL,
    birth_date DATE NOT NULL,
    first_name VARCHAR2(50) NOT NULL,
    last_name VARCHAR2(50) NOT NULL,
    sex VARCHAR2(1) NOT NULL,
    hire_date DATE NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (emp_no),
    CONSTRAINT fk_employees_titles FOREIGN KEY (emp_title_id) REFERENCES titles (title_id)
);

-- create department employees table
CREATE TABLE dept_emp (
    emp_no NUMBER NOT NULL,
    dept_no VARCHAR2(10) NOT NULL,
    CONSTRAINT pk_dept_emp PRIMARY KEY (emp_no, dept_no),
    CONSTRAINT fk_dept_emp_employees FOREIGN KEY (emp_no) REFERENCES employees (emp_no),
    CONSTRAINT fk_dept_emp_departments FOREIGN KEY (dept_no) REFERENCES departments (dept_no)
);

-- create department managers table
CREATE TABLE dept_manager (
    dept_no VARCHAR2(10) NOT NULL,
    emp_no NUMBER NOT NULL,
    CONSTRAINT pk_dept_manager PRIMARY KEY (dept_no, emp_no),
    CONSTRAINT fk_dept_manager_employees FOREIGN KEY (emp_no) REFERENCES employees (emp_no),
    CONSTRAINT fk_dept_manager_departments FOREIGN KEY (dept_no) REFERENCES departments (dept_no)
);

-- create salaries table
CREATE TABLE salaries (
    emp_no NUMBER NOT NULL,
    salary NUMBER NOT NULL,
    CONSTRAINT pk_salaries PRIMARY KEY (emp_no),
    CONSTRAINT fk_salaries_employees FOREIGN KEY (emp_no) REFERENCES employees (emp_no)
);
