// Consulta a API já autenticada. Não altera funcionários nem históricos.
const fs=require('node:fs');
const base=process.env.SPRINGBOOT_URL||'http://127.0.0.1:8080';
(async()=>{
 const login=await fetch(base+'/api/funcionario/login',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({email:process.env.RAIZ_ADMIN_EMAIL||'raiz@gmail.com',senha:process.env.RAIZ_ADMIN_SENHA||'12345'})});
 if(!login.ok)throw Error('Login administrativo: '+login.status);
 const cookie=login.headers.get('set-cookie').split(';')[0];
 try{
  async function get(path){const r=await fetch(base+path,{headers:{cookie}});if(!r.ok)throw Error(path+': '+r.status);return r.json();}
  const usuarios=await get('/api/funcionario/listar');
  const ativos=new Map(usuarios.filter(x=>x.active).map(x=>[x.employeeId,x]));
  let todos=[],painel;
  for(let page=0;;page++){
   const d=await get('/api/dashboard/ams?periodo=5&size=100&page='+page);painel=d;
   todos.push(...d.integrantes.content);
   if(todos.length>=d.integrantes.totalElements)break;
  }
  const completos=todos.filter(l=>ativos.get(l.pessoa.id)?.posicao==='COLABORADOR'&&l.calculo.dimensoes.every(d=>d.registrada!=null));
  const candidato=dev=>completos.find(l=>l.calculo.desenvolvedor===dev);
  const equipe=gestor=>painel.areas.find(g=>g.id===gestor.companyId+'/'+gestor.departmentId)?.evolucao[4].resumo;
  const gestores=usuarios.filter(u=>u.active&&u.posicao==='GESTOR'&&equipe(u)?.pessoas>=5&&equipe(u)?.nota!=null&&
   ['governance','finops'].every(id=>equipe(u).dimensoes.find(d=>d.id===id)?.validos>=0.8*equipe(u).pessoas))
   .sort((a,b)=>equipe(b).nota-equipe(a).nota);
  const pessoa=l=>l?{...ativos.get(l.pessoa.id),senha:undefined,ams:l.calculo.nota,dimensoes:l.calculo.dimensoes}:null;
  const resultado={periodo:5,desenvolvedor:pessoa(candidato(true)),colaborador:pessoa(candidato(false)),
   gestorMaior:{...gestores[0],equipe:equipe(gestores[0])},gestorMenor:{...gestores.at(-1),equipe:equipe(gestores.at(-1))}};
  fs.mkdirSync('../target',{recursive:true});fs.writeFileSync('../target/auth-candidates.json',JSON.stringify(resultado,null,2));
  console.log(JSON.stringify(resultado,null,2));
 }finally{await fetch(base+'/api/funcionario/logout',{method:'POST',headers:{cookie}});}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
