interface Project {
  name: string;
  values: Object;
  children: Project[];
}

interface Value {
  value: string;
  is_inherited: boolean;
}

export { Project, Value };
